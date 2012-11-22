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



#ifndef SkSize_DEFINED
#define SkSize_DEFINED

template <typename T> struct SkTSize {
    T fWidth;
    T fHeight;

    static SkTSize Make(T w, T h) {
        SkTSize s;
        s.fWidth = w;
        s.fHeight = h;
        return s;
    }

    void set(T w, T h) {
        fWidth = w;
        fHeight = h;
    }

    /** Returns true iff fWidth == 0 && fHeight == 0
     */
    bool isZero() const {
        return 0 == fWidth && 0 == fHeight;
    }

    /** Returns true if either widht or height are <= 0 */
    bool isEmpty() const {
        return fWidth <= 0 || fHeight <= 0;
    }
    
    /** Set the width and height to 0 */
    void setEmpty() {
        fWidth = fHeight = 0;
    }
	
	T width() const { return fWidth; }
	T height() const { return fHeight; }
    
    /** If width or height is < 0, it is set to 0 */
    void clampNegToZero() {
        if (fWidth < 0) {
            fWidth = 0;
        }
        if (fHeight < 0) {
            fHeight = 0;
        }
    }
    
    bool equals(T w, T h) const {
        return fWidth == w && fHeight == h;
    }
};

template <typename T>
static inline bool operator==(const SkTSize<T>& a, const SkTSize<T>& b) {
    return a.fWidth == b.fWidth && a.fHeight == b.fHeight;
}

template <typename T>
static inline bool operator!=(const SkTSize<T>& a, const SkTSize<T>& b) {
    return !(a == b);
}

///////////////////////////////////////////////////////////////////////////////

typedef SkTSize<int32_t> SkISize;

#include "SkScalar.h"

struct SkSize : public SkTSize<SkScalar> {
    static SkSize Make(SkScalar w, SkScalar h) {
        SkSize s;
        s.fWidth = w;
        s.fHeight = h;
        return s;
    }
    
    
    SkSize& operator=(const SkISize& src) {
        this->set(SkIntToScalar(src.fWidth), SkIntToScalar(src.fHeight));
        return *this;
    }

    SkISize round() const {
        SkISize s;
        s.set(SkScalarRound(fWidth), SkScalarRound(fHeight));
        return s;
    }
    
    SkISize ceil() const {
        SkISize s;
        s.set(SkScalarCeil(fWidth), SkScalarCeil(fHeight));
        return s;
    }

    SkISize floor() const {
        SkISize s;
        s.set(SkScalarFloor(fWidth), SkScalarFloor(fHeight));
        return s;
    }
};

#endif
