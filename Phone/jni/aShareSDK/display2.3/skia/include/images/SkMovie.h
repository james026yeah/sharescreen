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

#ifndef SkMovie_DEFINED
#define SkMovie_DEFINED

#include "SkRefCnt.h"
#include "SkCanvas.h"

class SkStream;

class SkMovie : public SkRefCnt {
public:
    /** Try to create a movie from the stream. If the stream format is not
        supported, return NULL.
    */
    static SkMovie* DecodeStream(SkStream*);
    /** Try to create a movie from the specified file path. If the file is not
        found, or the format is not supported, return NULL. If a movie is
        returned, the stream may be retained by the movie (via ref()) until
        the movie is finished with it (by calling unref()).
    */
    static SkMovie* DecodeFile(const char path[]);
    /** Try to create a movie from the specified memory.
        If the format is not supported, return NULL. If a movie is returned,
        the data will have been read or copied, and so the caller may free
        it.
    */
    static SkMovie* DecodeMemory(const void* data, size_t length);

    SkMSec  duration();
    int     width();
    int     height();
    int     isOpaque();
    
    /** Specify the time code (between 0...duration) to sample a bitmap
        from the movie. Returns true if this time code generated a different
        bitmap/frame from the previous state (i.e. true means you need to
        redraw).
    */
    bool setTime(SkMSec);

    //for add gif
    //the following 3 methods are intented for no one but Movie to use.
    //please see Movie.cpp for information
    virtual int getGifFrameDuration(int frameIndex);
    virtual int getGifTotalFrameCount();
    SkBitmap* createGifFrameBitmap();
    virtual bool setCurrFrame(int frameIndex);
    //for and gif end

    // return the right bitmap for the current time code
    const SkBitmap& bitmap();
    
protected:
    struct Info {
        SkMSec  fDuration;
        int     fWidth;
        int     fHeight;
        bool    fIsOpaque;
    };

    virtual bool onGetInfo(Info*) = 0;
    virtual bool onSetTime(SkMSec) = 0;
    virtual bool onGetBitmap(SkBitmap*) = 0;

    // visible for subclasses
    SkMovie();

private:
    Info        fInfo;
    SkMSec      fCurrTime;
    SkBitmap    fBitmap;
    bool        fNeedBitmap;
    
    void ensureInfo();
};

#endif
