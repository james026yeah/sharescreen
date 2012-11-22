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
 * Copyright (C) 2006 The Android Open Source Project
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

#ifndef SkFloatingPoint_DEFINED
#define SkFloatingPoint_DEFINED

#include "SkTypes.h"

#ifdef SK_CAN_USE_FLOAT

#include <math.h>
#include <float.h>
#include "SkFloatBits.h"

// If math.h had powf(float, float), I could remove this wrapper
static inline float sk_float_pow(float base, float exp) {
    return static_cast<float>(pow(static_cast<double>(base),
                                  static_cast<double>(exp)));
}

static inline float sk_float_copysign(float x, float y) {
    int32_t xbits = SkFloat2Bits(x);
    int32_t ybits = SkFloat2Bits(y);
    return SkBits2Float((xbits & 0x7FFFFFFF) | (ybits & 0x80000000));
}

#ifdef SK_BUILD_FOR_WINCE
    #define sk_float_sqrt(x)        (float)::sqrt(x)
    #define sk_float_sin(x)         (float)::sin(x)
    #define sk_float_cos(x)         (float)::cos(x)
    #define sk_float_tan(x)         (float)::tan(x)
    #define sk_float_acos(x)        (float)::acos(x)
    #define sk_float_asin(x)        (float)::asin(x)
    #define sk_float_atan2(y,x)     (float)::atan2(y,x)
    #define sk_float_abs(x)         (float)::fabs(x)
    #define sk_float_mod(x,y)       (float)::fmod(x,y)
    #define sk_float_exp(x)         (float)::exp(x)
    #define sk_float_log(x)         (float)::log(x)
    #define sk_float_floor(x)       (float)::floor(x)
    #define sk_float_ceil(x)        (float)::ceil(x)
#else
    #define sk_float_sqrt(x)        sqrtf(x)
    #define sk_float_sin(x)         sinf(x)
    #define sk_float_cos(x)         cosf(x)
    #define sk_float_tan(x)         tanf(x)
    #define sk_float_floor(x)       floorf(x)
    #define sk_float_ceil(x)        ceilf(x)
#ifdef SK_BUILD_FOR_MAC
    #define sk_float_acos(x)        static_cast<float>(acos(x))
    #define sk_float_asin(x)        static_cast<float>(asin(x))
#else
    #define sk_float_acos(x)        acosf(x)
    #define sk_float_asin(x)        asinf(x)
#endif
    #define sk_float_atan2(y,x) atan2f(y,x)
    #define sk_float_abs(x)         fabsf(x)
    #define sk_float_mod(x,y)       fmodf(x,y)
    #define sk_float_exp(x)         expf(x)
    #define sk_float_log(x)         logf(x)
    #define sk_float_isNaN(x)       _isnan(x)
#endif

#ifdef SK_USE_FLOATBITS
    #define sk_float_floor2int(x)   SkFloatToIntFloor(x)
    #define sk_float_round2int(x)   SkFloatToIntRound(x)
    #define sk_float_ceil2int(x)    SkFloatToIntCeil(x)
#else
    #define sk_float_floor2int(x)   (int)sk_float_floor(x)
    #define sk_float_round2int(x)   (int)sk_float_floor((x) + 0.5f)
    #define sk_float_ceil2int(x)    (int)sk_float_ceil(x)
#endif

#endif
#endif
