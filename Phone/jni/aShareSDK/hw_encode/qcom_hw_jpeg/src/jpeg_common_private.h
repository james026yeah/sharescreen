/*========================================================================

*//** @file jpeg_common_private.h  

@par EXTERNALIZED FUNCTIONS
  (none)

@par INITIALIZATION AND SEQUENCING REQUIREMENTS
  (none)

Copyright (C) 2008 QUALCOMM Incorporated.
All Rights Reserved. QUALCOMM Proprietary and Confidential.
*//*====================================================================== */

/*========================================================================
                             Edit History

$Header:

when       who     what, where, why
--------   ---     -------------------------------------------------------
08/03/09   vma     Switched to use the os abstraction layer (os_*)
09/07/08   vma     Created file.
========================================================================== */

#ifndef __JPEG_COMMON_PRIVATE_H__
#define __JPEG_COMMON_PRIVATE_H__

#include "jpeg_common.h"
#include "jpeg_debug.h"
#include "os_thread.h"

#define STD_MEMSET(p,i,n)          memset(p, i, n)
#define STD_MIN(a,b)               ((a < b) ? a : b)
#define STD_MAX(a,b)               ((a > b) ? a : b)
#define CLAMP(x,min,max)           {if ((x) < (min)) (x) = (min); \
                                    if ((x) > (max)) (x) = (max);}
#define STD_MEMMOVE(dest,src,size) memcpy(dest, src, size)

#define ROUND_TO_8(x)          ((((x) + 7) >> 3) << 3)
#define ROUND_TO_16(x)         ((((x) + 15) >> 4) << 4)

#define JPEG_FREE(p)               {if (p) jpeg_free(p); p = NULL;}
#define JPEG_MALLOC(s)             jpeg_malloc(s, __FILE__, __LINE__)

#endif // __JPEG_COMMON_PRIVATE_H__      
