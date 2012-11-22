/*========================================================================

*//** @file jpeglog.h

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
06/23/09   zhiminl Replaced #ifdef _ANDROID_ with #ifdef ANDROID.
06/02/09   vma     Created file.

========================================================================== */

#ifndef _JPEGLOG_H
#define _JPEGLOG_H

#include <stdio.h>

#undef JDBG
#ifdef LOG_DEBUG
    #ifdef ANDROID
        #define LOG_NIDEBUG 0
        #define LOG_TAG "mm-still"
        #include <utils/Log.h>
        #define JDBG(fmt, args...) LOGI(fmt, ##args)
    #else
        #define JDBG(fmt, args...) fprintf(stderr, fmt, ##args)
    #endif
#else
    #define JDBG(...) //
#endif

// MACROs for printing out debug messages
#define JPEG_DBG_ERROR(...)   JDBG(__VA_ARGS__)
#define JPEG_DBG_HIGH(...)    JDBG(__VA_ARGS__)
#define JPEG_DBG_MED(...)     JDBG(__VA_ARGS__)
#define JPEG_DBG_LOW(...)     //JDBG(__VA_ARGS__)

#endif /* _JPEGLOG_H */
