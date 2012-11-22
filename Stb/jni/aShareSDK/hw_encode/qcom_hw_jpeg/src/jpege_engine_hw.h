/*========================================================================


*//** @file jpege_engine_hw.h

@par EXTERNALIZED FUNCTIONS
  (none)

@par INITIALIZATION AND SEQUENCING REQUIREMENTS
  (none)

Copyright (c) 2010 QUALCOMM, Incorporated.  All Rights Reserved.
QUALCOMM Proprietary.  Export of this technology or software is regulated
by the U.S. Government, Diversion contrary to U.S. law prohibited.
*//*====================================================================== */

/*========================================================================
                             Edit History

$Header:

when       who     what, where, why
--------   ---     -------------------------------------------------------
04/29/10   staceyw Added flag to indicate output buffer whether is last
                   output buffer during output.
04/29/10   staceyw Created file.

========================================================================== */

#ifndef _JPEGE_ENGINE_HW_H
#define _JPEGE_ENGINE_HW_H

#include "os_thread.h"
#include "jpeg_common.h"
#include "jpege_engine.h"
#include "jpege.h"
#include "jpeg_buffer_private.h"
#include "../../gemini/inc/gemini_lib.h"

extern jpege_engine_profile_t jpege_engine_hw_profile;

typedef enum
{
    YCBCR,
    YCRCB

} hw_input_format_t;

typedef enum
{
    JPEGE_HW_UNINITIALIZED, // Uninitialized state
    JPEGE_HW_IDLE,          // Idle state
    JPEGE_HW_ENCODING,      // Encoding
    JPEGE_HW_ENCODE_WAIT,   // Waiting for ENCODE_ACK from hw
    JPEGE_HW_DONE_WAIT,     // Waiting for IDLE_ACK from hw after encoding is done
    JPEGE_HW_ABORT_WAIT,    // Waiting for IDLE_ACK from hw after encoding is aborted
} jpege_engine_hw_state_t;

typedef struct
{
    jpege_engine_obj_t           *p_wrapper;      // The wrapper engine object
    os_thread_t                   thread;         // The encode thread
    os_mutex_t                    mutex;          // os mutex object
    os_cond_t                     cond;           // os condition variable
    jpege_engine_dst_t            dest;           // Destination object
    jpege_engine_event_handler_t  p_handler;      // Event handler
    jpege_engine_hw_state_t       state;          // Engine state
    gmn_obj_t                     gmn_obj;        // Gemini object
	int                           gmn_fd;         // FD of gemini object
	jpeg_buf_t                   *p_hw_buf;       // PMEM buffer to hold output
    jpeg_buf_t                   *p_out_bufs[2];  // Internal output buffers ptr for verification of output buf
    uint8_t                       buf_index;      // Internal buf index for verification of output buf

	uint8_t                       output_done;
	uint8_t                       abort_flag;     // abort flag
	uint8_t                       error_flag;     // abort flag
    uint8_t                       is_active;      // is active

	jpeg_buf_t                   *p_input_luma;   // Input Y buffer ptr
	jpeg_buf_t                   *p_input_chroma; // Input CbCr buffer ptr

	uint32_t                      luma_width;     // Input Luma Width
    uint32_t                      luma_height;    // Input Luma Height

	uint32_t                      restart_interval;// restart interval in number of MCUs
    uint32_t                      rotation;       // clockwise rotation angle
    hw_input_format_t             input_format;   // Input File Format (YCbCr or YCrCb)
	jpeg_subsampling_t            subsampling;    // Subsampling format
	uint32_t                      quality;        // Quantization Quality Factor
	uint8_t                       input_pmem_flag;// flag is set to true if input is provided from PMEM

    uint32_t                      total_size_received; // Size in bytes received
    uint32_t                      output_buf_rcvd_cnt; // num output buffers received
    uint32_t                      num_output_buffers; // total num of o/p buffers
    os_mutex_t                    frame_done_mutex; /* os mutex object */
    os_cond_t                     frame_done_cond;  /* os condition variable*/
} jpege_engine_hw_t;

#endif /* _JPEGE_ENGINE_DSP_H */
