/* -*- Mode: c++ -*- 
 *
 *  Copyright 1997 Massachusetts Institute of Technology
 * 
 *  Permission to use, copy, modify, distribute, and sell this software and its
 *  documentation for any purpose is hereby granted without fee, provided that
 *  the above copyright notice appear in all copies and that both that
 *  copyright notice and this permission notice appear in supporting
 *  documentation, and that the name of M.I.T. not be used in advertising or
 *  publicity pertaining to distribution of the software without specific,
 *  written prior permission.  M.I.T. makes no representations about the
 *  suitability of this software for any purpose.  It is provided "as is"
 *  without express or implied warranty.
 * 
 */

#ifndef _VRCOMPLEXTOMAG_H_
#define _VRCOMPLEXTOMAG_H_

#include <VrSigProc.h>
#include <math.h>

class VrComplexToMag : public VrSigProc<float, float> 
{
public:
   virtual void work(int n) 
    {
      for (int j = 0; j < n; j++) { 
	outputWrite(sqrt(inputRead(0) * inputRead(-1)));
	incInput(2);
      }
    }
  
  virtual void initialize() 
    {
      setHistory(2);
    }
};



#endif
