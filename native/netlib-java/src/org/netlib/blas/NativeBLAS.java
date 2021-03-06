/*
 * Copyright 2003-2007 Keith Seymour.
 * Copyright 1992-2007 The University of Tennessee. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer listed
 *   in this license in the documentation and/or other materials
 *   provided with the distribution.
 * 
 * - Neither the name of the copyright holders nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * This file was auto-generated by the org.netlib.generate.JavaGenerator
 * program, a part of netlib-java.
 * 
 * @see http://code.google.com/p/netlib-java/
 */
package org.netlib.blas;

import java.util.logging.Logger;
import org.netlib.util.StringW;
import org.netlib.util.booleanW;
import org.netlib.util.doubleW;
import org.netlib.util.floatW;
import org.netlib.util.intW;
import org.netlib.utils.JNIMethods;

/**
 * BLAS provider implementation which uses the Java Native Interface to access
 * system netlib libraries.
 *
 * @see http://www.netlib.org/
 * @author Samuel Halliday
 */
final class NativeBLAS extends BLAS {

	// singleton
	protected static final NativeBLAS INSTANCE = new NativeBLAS();

	// indicates if the JNI loaded OK. If this is false, calls to the native
	// methods will fail with UnsatisfiedLinkError
	protected final boolean isLoaded;

	private NativeBLAS() {
		String libname = JNIMethods.getPortableLibraryName("jniblas");
		boolean succeeded=false;
		try {
			System.loadLibrary(libname);
			succeeded=true;
		} catch (UnsatisfiedLinkError e) {
		} catch (SecurityException e) {
		}
		isLoaded = succeeded;
	}

	@Override
	public native double dasum(int n, double[] dx, int incx);

	@Override
	public native void daxpy(int n, double da, double[] dx, int incx, double[] dy, int incy);

	@Override
	public native void dcopy(int n, double[] dx, int incx, double[] dy, int incy);

	@Override
	public native double ddot(int n, double[] dx, int incx, double[] dy, int incy);

	@Override
	public native void dgbmv(String trans, int m, int n, int kl, int ku, double alpha, double[] a, int lda, double[] x, int incx, double beta, double[] y, int incy);

	@Override
	public native void dgemm(String transa, String transb, int m, int n, int k, double alpha, double[] a, int lda, double[] b, int ldb, double beta, double[] c, int Ldc);

	@Override
	public native void dgemv(String trans, int m, int n, double alpha, double[] a, int lda, double[] x, int incx, double beta, double[] y, int incy);

	@Override
	public native void dger(int m, int n, double alpha, double[] x, int incx, double[] y, int incy, double[] a, int lda);

	@Override
	public native double dnrm2(int n, double[] x, int incx);

	@Override
	public native void drot(int n, double[] dx, int incx, double[] dy, int incy, double c, double s);

	@Override
	public native void drotg(doubleW da, doubleW db, doubleW c, doubleW s);

	@Override
	public native void drotm(int n, double[] dx, int incx, double[] dy, int incy, double[] dparam);

	@Override
	public native void drotmg(doubleW dd1, doubleW dd2, doubleW dx1, double dy1, double[] dparam);

	@Override
	public native void dsbmv(String uplo, int n, int k, double alpha, double[] a, int lda, double[] x, int incx, double beta, double[] y, int incy);

	@Override
	public native void dscal(int n, double da, double[] dx, int incx);

	@Override
	public native void dspmv(String uplo, int n, double alpha, double[] ap, double[] x, int incx, double beta, double[] y, int incy);

	@Override
	public native void dspr(String uplo, int n, double alpha, double[] x, int incx, double[] ap);

	@Override
	public native void dspr2(String uplo, int n, double alpha, double[] x, int incx, double[] y, int incy, double[] ap);

	@Override
	public native void dswap(int n, double[] dx, int incx, double[] dy, int incy);

	@Override
	public native void dsymm(String side, String uplo, int m, int n, double alpha, double[] a, int lda, double[] b, int ldb, double beta, double[] c, int Ldc);

	@Override
	public native void dsymv(String uplo, int n, double alpha, double[] a, int lda, double[] x, int incx, double beta, double[] y, int incy);

	@Override
	public native void dsyr(String uplo, int n, double alpha, double[] x, int incx, double[] a, int lda);

	@Override
	public native void dsyr2(String uplo, int n, double alpha, double[] x, int incx, double[] y, int incy, double[] a, int lda);

	@Override
	public native void dsyr2k(String uplo, String trans, int n, int k, double alpha, double[] a, int lda, double[] b, int ldb, double beta, double[] c, int Ldc);

	@Override
	public native void dsyrk(String uplo, String trans, int n, int k, double alpha, double[] a, int lda, double beta, double[] c, int Ldc);

	@Override
	public native void dtbmv(String uplo, String trans, String diag, int n, int k, double[] a, int lda, double[] x, int incx);

	@Override
	public native void dtbsv(String uplo, String trans, String diag, int n, int k, double[] a, int lda, double[] x, int incx);

	@Override
	public native void dtpmv(String uplo, String trans, String diag, int n, double[] ap, double[] x, int incx);

	@Override
	public native void dtpsv(String uplo, String trans, String diag, int n, double[] ap, double[] x, int incx);

	@Override
	public native void dtrmm(String side, String uplo, String transa, String diag, int m, int n, double alpha, double[] a, int lda, double[] b, int ldb);

	@Override
	public native void dtrmv(String uplo, String trans, String diag, int n, double[] a, int lda, double[] x, int incx);

	@Override
	public native void dtrsm(String side, String uplo, String transa, String diag, int m, int n, double alpha, double[] a, int lda, double[] b, int ldb);

	@Override
	public native void dtrsv(String uplo, String trans, String diag, int n, double[] a, int lda, double[] x, int incx);

	@Override
	public native int idamax(int n, double[] dx, int incx);

	@Override
	public native int isamax(int n, float[] sx, int incx);

	@Override
	public native float sasum(int n, float[] sx, int incx);

	@Override
	public native void saxpy(int n, float sa, float[] sx, int incx, float[] sy, int incy);

	@Override
	public native void scopy(int n, float[] sx, int incx, float[] sy, int incy);

	@Override
	public native float sdot(int n, float[] sx, int incx, float[] sy, int incy);

	@Override
	public native float sdsdot(int n, float sb, float[] sx, int incx, float[] sy, int incy);

	@Override
	public native void sgbmv(String trans, int m, int n, int kl, int ku, float alpha, float[] a, int lda, float[] x, int incx, float beta, float[] y, int incy);

	@Override
	public native void sgemm(String transa, String transb, int m, int n, int k, float alpha, float[] a, int lda, float[] b, int ldb, float beta, float[] c, int Ldc);

	@Override
	public native void sgemv(String trans, int m, int n, float alpha, float[] a, int lda, float[] x, int incx, float beta, float[] y, int incy);

	@Override
	public native void sger(int m, int n, float alpha, float[] x, int incx, float[] y, int incy, float[] a, int lda);

	@Override
	public native float snrm2(int n, float[] x, int incx);

	@Override
	public native void srot(int n, float[] sx, int incx, float[] sy, int incy, float c, float s);

	@Override
	public native void srotg(floatW sa, floatW sb, floatW c, floatW s);

	@Override
	public native void srotm(int n, float[] sx, int incx, float[] sy, int incy, float[] sparam);

	@Override
	public native void srotmg(floatW sd1, floatW sd2, floatW sx1, float sy1, float[] sparam);

	@Override
	public native void ssbmv(String uplo, int n, int k, float alpha, float[] a, int lda, float[] x, int incx, float beta, float[] y, int incy);

	@Override
	public native void sscal(int n, float sa, float[] sx, int incx);

	@Override
	public native void sspmv(String uplo, int n, float alpha, float[] ap, float[] x, int incx, float beta, float[] y, int incy);

	@Override
	public native void sspr(String uplo, int n, float alpha, float[] x, int incx, float[] ap);

	@Override
	public native void sspr2(String uplo, int n, float alpha, float[] x, int incx, float[] y, int incy, float[] ap);

	@Override
	public native void sswap(int n, float[] sx, int incx, float[] sy, int incy);

	@Override
	public native void ssymm(String side, String uplo, int m, int n, float alpha, float[] a, int lda, float[] b, int ldb, float beta, float[] c, int Ldc);

	@Override
	public native void ssymv(String uplo, int n, float alpha, float[] a, int lda, float[] x, int incx, float beta, float[] y, int incy);

	@Override
	public native void ssyr(String uplo, int n, float alpha, float[] x, int incx, float[] a, int lda);

	@Override
	public native void ssyr2(String uplo, int n, float alpha, float[] x, int incx, float[] y, int incy, float[] a, int lda);

	@Override
	public native void ssyr2k(String uplo, String trans, int n, int k, float alpha, float[] a, int lda, float[] b, int ldb, float beta, float[] c, int Ldc);

	@Override
	public native void ssyrk(String uplo, String trans, int n, int k, float alpha, float[] a, int lda, float beta, float[] c, int Ldc);

	@Override
	public native void stbmv(String uplo, String trans, String diag, int n, int k, float[] a, int lda, float[] x, int incx);

	@Override
	public native void stbsv(String uplo, String trans, String diag, int n, int k, float[] a, int lda, float[] x, int incx);

	@Override
	public native void stpmv(String uplo, String trans, String diag, int n, float[] ap, float[] x, int incx);

	@Override
	public native void stpsv(String uplo, String trans, String diag, int n, float[] ap, float[] x, int incx);

	@Override
	public native void strmm(String side, String uplo, String transa, String diag, int m, int n, float alpha, float[] a, int lda, float[] b, int ldb);

	@Override
	public native void strmv(String uplo, String trans, String diag, int n, float[] a, int lda, float[] x, int incx);

	@Override
	public native void strsm(String side, String uplo, String transa, String diag, int m, int n, float alpha, float[] a, int lda, float[] b, int ldb);

	@Override
	public native void strsv(String uplo, String trans, String diag, int n, float[] a, int lda, float[] x, int incx);

}
