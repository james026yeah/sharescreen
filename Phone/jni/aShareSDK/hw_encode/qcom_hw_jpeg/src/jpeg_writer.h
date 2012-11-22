#ifndef _JPEG_WRITER_H
#define _JPEG_WRITER_H
/*========================================================================

                 C o m m o n   D e f i n i t i o n s

*//** @file jpeg_writer.h

Copyright (c) 1991-1998, Thomas G. Lane
See the IJG_README.txt file for more details.
Copyright (C) 2008 QUALCOMM Incorporated.
All Rights Reserved. QUALCOMM Proprietary and Confidential.

*//*====================================================================== */

/*========================================================================
                             Edit History

$Header:

when       who     what, where, why
--------   ---     -------------------------------------------------------
11/16/09   vma     Do not emit GPS IFD if there is no GPS tags inserted.
10/13/09   vma     Changed to pass output to upper layer (encoder) instead
                   of outputting through destination callback directly.
07/07/08   vma     Created file.

========================================================================== */

/* =======================================================================

                     INCLUDE FILES FOR MODULE

========================================================================== */
#include "jpege.h"
#include "jpeg_buffer_private.h"
#include "exif_private.h"

/* =======================================================================

                        DATA DECLARATIONS

========================================================================== */
/* -----------------------------------------------------------------------
** Constant / Define Declarations
** ----------------------------------------------------------------------- */
#define M_SOF0    0xc0
#define M_SOF1    0xc1
#define M_SOF2    0xc2
#define M_SOF3    0xc3

#define M_SOF5    0xc5
#define M_SOF6    0xc6
#define M_SOF7    0xc7

#define M_JPG     0xc8
#define M_SOF9    0xc9
#define M_SOF10   0xca
#define M_SOF11   0xcb

#define M_SOF13   0xcd
#define M_SOF14   0xce
#define M_SOF15   0xcf

#define M_DHT     0xc4

#define M_DAC     0xcc

#define M_RST0    0xd0
#define M_RST1    0xd1
#define M_RST2    0xd2
#define M_RST3    0xd3
#define M_RST4    0xd4
#define M_RST5    0xd5
#define M_RST6    0xd6
#define M_RST7    0xd7

#define M_SOI     0xd8
#define M_EOI     0xd9
#define M_SOS     0xda
#define M_DQT     0xdb
#define M_DNL     0xdc
#define M_DRI     0xdd
#define M_DHP     0xde
#define M_EXP     0xdf

#define M_APP0    0xe0
#define M_APP1    0xe1
#define M_APP2    0xe2
#define M_APP3    0xe3
#define M_APP4    0xe4
#define M_APP5    0xe5
#define M_APP6    0xe6
#define M_APP7    0xe7
#define M_APP8    0xe8
#define M_APP9    0xe9
#define M_APP10   0xea
#define M_APP11   0xeb
#define M_APP12   0xec
#define M_APP13   0xed
#define M_APP14   0xee
#define M_APP15   0xef

#define M_JPG0    0xf0
#define M_JPG13   0xfd
#define M_COM     0xfe

#define M_TEM     0x01
/* -----------------------------------------------------------------------
** Type Declarations
** ----------------------------------------------------------------------- */
typedef uint8_t jpeg_marker_t;

typedef void (*jpegw_output_handler_t)(jpege_obj_t encoder, jpeg_buf_t *p_buf);

typedef struct
{
    // Output handler
    jpegw_output_handler_t  p_handler;

    // Scratch buffer
    jpeg_buf_t *scratchBuf;

    // Write ahead buffer
    jpeg_buf_t *aheadBuf;

    // Thumbnail buffer
    jpeg_buf_t *thumbnailBuf;

    // Save the Tiff header location
    uint32_t nTiffHeaderOffset;

    // Save the App1 length location
    uint32_t nApp1LengthOffset;

    // Save the thumbnail starting location
    uint32_t nThumbnailOffset;

    // Save the thumbnail stream starting location
    uint32_t nThumbnailStreamOffset;

    // Save the Gps Ifd pointer location
    uint32_t nGpsIfdPointerOffset;

    // Save the Thumbnail Ifd pointer location
    uint32_t nThumbnailIfdPointerOffset;

    // Keep count of how many tags are written
    uint32_t nFieldCount;

    // Save the location to write the IFD count
    uint32_t nFieldCountOffset;

    // Save the offset in ThumbnailIfd specifying the end of thumbnail offset
    uint32_t nJpegInterchangeLOffset;

    // Number of GPS tags present to be emitted
    uint16_t nGpsTagsToEmit;

    // Flag indicating whether header is written
    uint8_t fHeaderWritten;

    // Flag indicating if App1 header is present
    uint8_t fApp1Present;

    // The Jpeg Encoder object
    jpege_obj_t  encoder;

    // The image source object
    jpege_src_t *p_source;

    // The destination object
    jpege_dst_t *p_dest;

    // The encode configuration
    jpege_cfg_t *p_encode_cfg;

    // The Exif Info object
    exif_info_t *p_exif_info;

    // Flag indicating whether the exif info object is owned by the writer
    uint8_t is_exif_info_owned;

    // Flag indicating whether any overflow has occurred
    uint8_t overflow_flag;

} jpeg_writer_t;

/* =======================================================================
**                          Macro Definitions
** ======================================================================= */
#define JPEG_EXIF_HEADER_SIZE 2000   /* Enough to hold GPS and PIM data  */
#define JPEG_JFIF_HEADER_SIZE  600
/* =======================================================================
**                          Function Declarations
** ======================================================================= */

int  jpegw_init               (jpeg_writer_t            *p_writer,
                               jpege_obj_t               encoder,
                               jpegw_output_handler_t    p_handler);
void jpegw_configure          (jpeg_writer_t            *p_writer,
                               jpege_src_t              *p_source,
                               jpege_dst_t              *p_dest,
                               jpege_cfg_t              *p_encode_cfg,
                               exif_info_obj_t          *p_exif_info_obj);
void jpegw_reset              (jpeg_writer_t            *p_writer);
int  jpegw_emit_header        (jpeg_writer_t            *p_writer);
void jpegw_emit_frame_header  (jpeg_writer_t            *p_writer,
                               jpege_img_cfg_t          *p_config,
                               jpege_img_data_t         *p_src);
void jpegw_emit_scan_header   (jpeg_writer_t            *p_writer,
                               const jpege_img_cfg_t    *p_config);
int  jpegw_emit_thumbnail     (jpeg_writer_t            *p_writer,
                               const uint8_t            *p_data,
                               uint32_t                  size);
void jpegw_destroy            (jpeg_writer_t            *p_writer);

#endif /* _JPEG_WRITER_H */
