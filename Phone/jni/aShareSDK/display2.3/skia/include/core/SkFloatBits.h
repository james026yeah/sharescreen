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

#ifndef SkFloatBits_DEFINED
#define SkFloatBits_DEFINED

#include "SkTypes.h"

/** Convert a sign-bit int (i.e. float interpreted as int) into a 2s compliement
    int. This also converts -0 (0x80000000) to 0. Doing this to a float allows
    it to be compared using normal C operators (<, <=, etc.)
*/
static inline int32_t SkSignBitTo2sCompliment(int32_t x) {
    if (x < 0) {
        x &= 0x7FFFFFFF;
        x = -x;
    }
    return x;
}

/** Convert a 2s compliment int to a sign-bit (i.e. int interpreted as float).
    This undoes the result of SkSignBitTo2sCompliment().
 */
static inline int32_t Sk2sComplimentToSignBit(int32_t x) {
    int sign = x >> 31;
    // make x positive
    x = (x ^ sign) - sign;
    // set the sign bit as needed
    x |= sign << 31;
    return x;
}

/** Given the bit representation of a float, return its value cast to an int.
    If the value is out of range, or NaN, return return +/- SK_MaxS32
*/
int32_t SkFloatBits_toIntCast(int32_t floatBits);

/** Given the bit representation of a float, return its floor as an int.
    If the value is out of range, or NaN, return return +/- SK_MaxS32
 */
int32_t SkFloatBits_toIntFloor(int32_t floatBits);

/** Given the bit representation of a float, return it rounded to an int.
    If the value is out of range, or NaN, return return +/- SK_MaxS32
 */
int32_t SkFloatBits_toIntRound(int32_t floatBits);

/** Given the bit representation of a float, return its ceiling as an int.
    If the value is out of range, or NaN, return return +/- SK_MaxS32
 */
int32_t SkFloatBits_toIntCeil(int32_t floatBits);


#ifdef SK_CAN_USE_FLOAT

union SkFloatIntUnion {
    float   fFloat;
    int32_t fSignBitInt;
};

// Helper to see a float as its bit pattern (w/o aliasing warnings)
static inline int32_t SkFloat2Bits(float x) {
    SkFloatIntUnion data;
    data.fFloat = x;
    return data.fSignBitInt;
}

// Helper to see a bit pattern as a float (w/o aliasing warnings)
static inline float SkBits2Float(int32_t floatAsBits) {
    SkFloatIntUnion data;
    data.fSignBitInt = floatAsBits;
    return data.fFloat;
}

/** Return the float as a 2s compliment int. Just to be used to compare floats
    to each other or against positive float-bit-constants (like 0). This does
    not return the int equivalent of the float, just something cheaper for
    compares-only.
 */
static inline int32_t SkFloatAs2sCompliment(float x) {
    return SkSignBitTo2sCompliment(SkFloat2Bits(x));
}

/** Return the 2s compliment int as a float. This undos the result of
    SkFloatAs2sCompliment
 */
static inline float Sk2sComplimentAsFloat(int32_t x) {
    return SkBits2Float(Sk2sComplimentToSignBit(x));
}

/** Return x cast to a float (i.e. (float)x)
*/
float SkIntToFloatCast(int x);
float SkIntToFloatCast_NoOverflowCheck(int x);

/** Return the float cast to an int.
    If the value is out of range, or NaN, return +/- SK_MaxS32
*/
static inline int32_t SkFloatToIntCast(float x) {
    return SkFloatBits_toIntCast(SkFloat2Bits(x));
}

/** Return the floor of the float as an int.
    If the value is out of range, or NaN, return +/- SK_MaxS32
*/
static inline int32_t SkFloatToIntFloor(float x) {
    return SkFloatBits_toIntFloor(SkFloat2Bits(x));
}

/** Return the float rounded to an int.
    If the value is out of range, or NaN, return +/- SK_MaxS32
*/
static inline int32_t SkFloatToIntRound(float x) {
    return SkFloatBits_toIntRound(SkFloat2Bits(x));
}

/** Return the ceiling of the float as an int.
    If the value is out of range, or NaN, return +/- SK_MaxS32
*/
static inline int32_t SkFloatToIntCeil(float x) {
    return SkFloatBits_toIntCeil(SkFloat2Bits(x));
}

#endif

//  Scalar wrappers for float-bit routines

#ifdef SK_SCALAR_IS_FLOAT
    #define SkScalarAs2sCompliment(x)    SkFloatAs2sCompliment(x)
    #define Sk2sComplimentAsScalar(x)    Sk2sComplimentAsFloat(x)
#else
    #define SkScalarAs2sCompliment(x)    (x)
    #define Sk2sComplimentAsScalar(x)    (x)
#endif

#endif

