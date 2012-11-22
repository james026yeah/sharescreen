/*========================================================================

*//** @file os_pmem.c

@par EXTERNALIZED FUNCTIONS
  (none)

@par INITIALIZATION AND SEQUENCING REQUIREMENTS
  (none)

Copyright (C) 2009 QUALCOMM Incorporated.
All Rights Reserved. QUALCOMM Proprietary and Confidential.
*//*====================================================================== */

/*========================================================================
                             Edit History

$Header:

when       who     what, where, why
--------   ---     -------------------------------------------------------
08/11/09   vma     Removed 2^n restriction on PMEM (clp2)
05/12/09   vma     Created file.

========================================================================== */

#include "os_pmem.h"
#include "jpegerr.h"
#include "jpeglog.h"
#include <string.h>

int os_pmem_fd_open(pmem_fd_t *p_pmem_fd)
{
    if (!p_pmem_fd)
        return JPEGERR_EFAILED;
    char* pmem_region;
#ifdef USE_PMEM_SMI
    pmem_region = "/dev/pmem_smipool";
#else
    pmem_region = "/dev/pmem_adsp";
#endif

    *p_pmem_fd = open(pmem_region, O_RDWR|O_SYNC);
    if (*p_pmem_fd < 0)
    {
        JPEG_DBG_HIGH("os_pmem_fd_open: failed to open pmem (%d - %s)\n",
                       errno, strerror(errno));
        return JPEGERR_EFAILED;
    }
    return JPEGERR_SUCCESS;
}

int os_pmem_fd_close(pmem_fd_t *p_pmem_fd)
{
    if (!p_pmem_fd || *p_pmem_fd < 0)
        return JPEGERR_EFAILED; 

    (void)close(*p_pmem_fd);
    *p_pmem_fd = OS_PMEM_FD_INIT_VALUE;
    return JPEGERR_SUCCESS;
}

int os_pmem_allocate(pmem_fd_t pmem_fd, uint32_t size, uint8_t **p_vaddr) 
{
    if (pmem_fd <= 0 || !p_vaddr)
        return JPEGERR_EFAILED;

    ALIGN(size, PAGESIZE);
    *p_vaddr = mmap(NULL, size, PROT_READ | PROT_WRITE,
                    MAP_SHARED, pmem_fd, 0);
    if (*p_vaddr == MAP_FAILED)       
    {
       return JPEGERR_EMALLOC;
    }
    return JPEGERR_SUCCESS;
}

int os_pmem_free(pmem_fd_t pmem_fd, uint32_t size, uint8_t *vaddr)
{
    if (pmem_fd <= 0 || !vaddr)
        return JPEGERR_EFAILED;

    ALIGN(size, PAGESIZE);
    return munmap(vaddr, size);
}

int os_pmem_get_phy_addr(pmem_fd_t pmem_fd, uint8_t **p_paddr)
{
    if (pmem_fd <= 0 || !p_paddr)
        return JPEGERR_EFAILED;
 
    if (ioctl(pmem_fd, PMEM_GET_PHYS, p_paddr) < 0)
        return JPEGERR_EFAILED;

    return JPEGERR_SUCCESS;
}
