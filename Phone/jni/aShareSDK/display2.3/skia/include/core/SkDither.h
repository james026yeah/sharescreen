/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef SkDither_DEFINED
#define SkDither_DEFINED

#include "SkColorPriv.h"

#define SK_DitherValueMax4444   15
#define SK_DitherValueMax565    7

/*  need to use macros for bit-counts for each component, and then
    move these into SkColorPriv.h
*/

#define SkDITHER_R32_FOR_565_MACRO(r, d)    (r + d - (r >> 5))
#define SkDITHER_G32_FOR_565_MACRO(g, d)    (g + (d >> 1) - (g >> 6))
#define SkDITHER_B32_FOR_565_MACRO(b, d)    (b + d - (b >> 5))

#define SkDITHER_A32_FOR_4444_MACRO(a, d)    (a + 15 - (a >> 4))
#define SkDITHER_R32_FOR_4444_MACRO(r, d)    (r + d - (r >> 4))
#define SkDITHER_G32_FOR_4444_MACRO(g, d)    (g + d - (g >> 4))
#define SkDITHER_B32_FOR_4444_MACRO(b, d)    (b + d - (b >> 4))

#ifdef SK_DEBUG
    inline unsigned SkDITHER_R32_FOR_565(unsigned r, unsigned d)
    {
        SkASSERT(d <= SK_DitherValueMax565);
        SkA32Assert(r);
        r = SkDITHER_R32_FOR_565_MACRO(r, d);
        SkA32Assert(r);
        return r;
    }
    inline unsigned SkDITHER_G32_FOR_565(unsigned g, unsigned d)
    {
        SkASSERT(d <= SK_DitherValueMax565);
        SkG32Assert(g);
        g = SkDITHER_G32_FOR_565_MACRO(g, d);
        SkG32Assert(g);
        return g;
    }
    inline unsigned SkDITHER_B32_FOR_565(unsigned b, unsigned d)
    {
        SkASSERT(d <= SK_DitherValueMax565);
        SkB32Assert(b);
        b = SkDITHER_B32_FOR_565_MACRO(b, d);
        SkB32Assert(b);
        return b;
    }
#else
    #define SkDITHER_R32_FOR_565(r, d)  SkDITHER_R32_FOR_565_MACRO(r, d)
    #define SkDITHER_G32_FOR_565(g, d)  SkDITHER_G32_FOR_565_MACRO(g, d)
    #define SkDITHER_B32_FOR_565(b, d)  SkDITHER_B32_FOR_565_MACRO(b, d)
#endif

#define SkDITHER_R32To565(r, d)  SkR32ToR16(SkDITHER_R32_FOR_565(r, d))
#define SkDITHER_G32To565(g, d)  SkG32ToG16(SkDITHER_G32_FOR_565(g, d))
#define SkDITHER_B32To565(b, d)  SkB32ToB16(SkDITHER_B32_FOR_565(b, d))

#define SkDITHER_A32To4444(a, d)  SkA32To4444(SkDITHER_A32_FOR_4444_MACRO(a, d))
#define SkDITHER_R32To4444(r, d)  SkR32To4444(SkDITHER_R32_FOR_4444_MACRO(r, d))
#define SkDITHER_G32To4444(g, d)  SkG32To4444(SkDITHER_G32_FOR_4444_MACRO(g, d))
#define SkDITHER_B32To4444(b, d)  SkB32To4444(SkDITHER_B32_FOR_4444_MACRO(b, d))

static inline SkPMColor SkDitherARGB32For565(SkPMColor c, unsigned dither)
{
    SkASSERT(dither <= SK_DitherValueMax565);
    
    unsigned sa = SkGetPackedA32(c);
    dither = SkAlphaMul(dither, SkAlpha255To256(sa));

    unsigned sr = SkGetPackedR32(c);
    unsigned sg = SkGetPackedG32(c);
    unsigned sb = SkGetPackedB32(c);
    sr = SkDITHER_R32_FOR_565(sr, dither);
    sg = SkDITHER_G32_FOR_565(sg, dither);
    sb = SkDITHER_B32_FOR_565(sb, dither);
    
    return SkPackARGB32(sa, sr, sg, sb);
}

static inline SkPMColor SkDitherRGB32For565(SkPMColor c, unsigned dither)
{
    SkASSERT(dither <= SK_DitherValueMax565);
    
    unsigned sr = SkGetPackedR32(c);
    unsigned sg = SkGetPackedG32(c);
    unsigned sb = SkGetPackedB32(c);
    sr = SkDITHER_R32_FOR_565(sr, dither);
    sg = SkDITHER_G32_FOR_565(sg, dither);
    sb = SkDITHER_B32_FOR_565(sb, dither);
    
    return SkPackARGB32(0xFF, sr, sg, sb);
}

static inline uint16_t SkDitherRGBTo565(U8CPU r, U8CPU g, U8CPU b,
                                              unsigned dither)
{
    SkASSERT(dither <= SK_DitherValueMax565);
    r = SkDITHER_R32To565(r, dither);
    g = SkDITHER_G32To565(g, dither);
    b = SkDITHER_B32To565(b, dither);
    return SkPackRGB16(r, g, b);
}

static inline uint16_t SkDitherRGB32To565(SkPMColor c, unsigned dither)
{
    SkASSERT(dither <= SK_DitherValueMax565);
    
    unsigned sr = SkGetPackedR32(c);
    unsigned sg = SkGetPackedG32(c);
    unsigned sb = SkGetPackedB32(c);
    sr = SkDITHER_R32To565(sr, dither);
    sg = SkDITHER_G32To565(sg, dither);
    sb = SkDITHER_B32To565(sb, dither);
    
    return SkPackRGB16(sr, sg, sb);
}

static inline uint16_t SkDitherARGB32To565(U8CPU sa, SkPMColor c, unsigned dither)
{
    SkASSERT(dither <= SK_DitherValueMax565);    
    dither = SkAlphaMul(dither, SkAlpha255To256(sa));
    
    unsigned sr = SkGetPackedR32(c);
    unsigned sg = SkGetPackedG32(c);
    unsigned sb = SkGetPackedB32(c);
    sr = SkDITHER_R32To565(sr, dither);
    sg = SkDITHER_G32To565(sg, dither);
    sb = SkDITHER_B32To565(sb, dither);
    
    return SkPackRGB16(sr, sg, sb);
}

///////////////////////// 4444

static inline SkPMColor16 SkDitherARGB32To4444(U8CPU a, U8CPU r, U8CPU g,
                                               U8CPU b, unsigned dither)
{
    dither = SkAlphaMul(dither, SkAlpha255To256(a));

    a = SkDITHER_A32To4444(a, dither);
    r = SkDITHER_R32To4444(r, dither);
    g = SkDITHER_G32To4444(g, dither);
    b = SkDITHER_B32To4444(b, dither);
    
    return SkPackARGB4444(a, r, g, b);
}

static inline SkPMColor16 SkDitherARGB32To4444(SkPMColor c, unsigned dither)
{
    unsigned a = SkGetPackedA32(c);
    unsigned r = SkGetPackedR32(c);
    unsigned g = SkGetPackedG32(c);
    unsigned b = SkGetPackedB32(c);

    dither = SkAlphaMul(dither, SkAlpha255To256(a));

    a = SkDITHER_A32To4444(a, dither);
    r = SkDITHER_R32To4444(r, dither);
    g = SkDITHER_G32To4444(g, dither);
    b = SkDITHER_B32To4444(b, dither);
    
    return SkPackARGB4444(a, r, g, b);
}

// TODO: need dither routines for 565 -> 4444

// this toggles between a 4x4 and a 1x4 array
//#define ENABLE_DITHER_MATRIX_4X4

#ifdef ENABLE_DITHER_MATRIX_4X4
    extern const uint8_t gDitherMatrix_4Bit_4X4[4][4];
    extern const uint8_t gDitherMatrix_3Bit_4X4[4][4];

    #define DITHER_4444_SCAN(y) const uint8_t* dither_scan = gDitherMatrix_4Bit_4X4[(y) & 3]
    #define DITHER_565_SCAN(y)  const uint8_t* dither_scan = gDitherMatrix_3Bit_4X4[(y) & 3]

    #define DITHER_VALUE(x) dither_scan[(x) & 3]
#else
    extern const uint16_t gDitherMatrix_4Bit_16[4];
    extern const uint16_t gDitherMatrix_3Bit_16[4];

    #define DITHER_4444_SCAN(y) const uint16_t dither_scan = gDitherMatrix_4Bit_16[(y) & 3]
    #define DITHER_565_SCAN(y)  const uint16_t dither_scan = gDitherMatrix_3Bit_16[(y) & 3]

    #define DITHER_VALUE(x) ((dither_scan >> (((x) & 3) << 2)) & 0xF)
#endif

#define DITHER_INC_X(x) ++(x)

#endif
