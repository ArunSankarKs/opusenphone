/*
This file is part of Socks via HTTP.

This package is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

Socks via HTTP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Socks via HTTP; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

// Title :        ByteUtils.java
// Version :      1.2
// Copyright :    Copyright (c) 2001-2002
// Author :       Florent CUETO  & Sebastien LEBRETON <socksviahttp@cqs.dyndns.org>
// Description :  Class to manipulate bytes

package socksviahttp.core.util;

import java.io.*;
import java.util.zip.*;

public class ByteUtils
{
  public ByteUtils()
  {
    super();
  }

  public static int b2i(byte b)
  {
    return (b < 0 ? 256 + b : b);
  }

  public static byte i2b(int i)
  {
    return (i > 127 ? (byte)(i - 256) : (byte)i);
  }

  // Build a byte from 8 booleans
  public static byte byteFromBooleans(boolean b8, boolean b7, boolean b6, boolean b5, boolean b4, boolean b3, boolean b2, boolean b1)
  {
    byte b = 0;
    b += (b8 ? 128 : 0);
    b += (b7 ? 64 : 0);
    b += (b6 ? 32 : 0);
    b += (b5 ? 16 : 0);
    b += (b4 ? 8 : 0);
    b += (b3 ? 4 : 0);
    b += (b2 ? 2 : 0);
    b += (b1 ? 1 : 0);
    return(b);
  }

  // Reverse mechanism
  public static boolean[] booleansFromByte(byte b)
  {
    boolean[] ret = new boolean[8];
    ret[0] = (b&1) != 0;
    ret[1] = (b&2) != 0;
    ret[2] = (b&4) != 0;
    ret[3] = (b&8) != 0;
    ret[4] = (b&16) != 0;
    ret[5] = (b&32) != 0;
    ret[6] = (b&64) != 0;
    ret[7] = (b&128) != 0;
    return(ret);
  }

  public static long longFromBytes(byte b8, byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1)
  {
    long l = 0;
    l += (((long)b1) & 0xFF);
    l += (((long)b2) & 0xFF) << 8;
    l += (((long)b3) & 0xFF) << 16;
    l += (((long)b4) & 0xFF) << 24;
    l += (((long)b5) & 0xFF) << 32;
    l += (((long)b6) & 0xFF) << 40;
    l += (((long)b7) & 0xFF) << 48;
    l += (((long)b8) & 0xFF) << 56;
    return(l);
  }

  public static byte[] bytesFromLong(long l)
  {
    byte[] ret = new byte[8];
    for (int i = 0; i < 8; i++)
    {
      ret[i] = (byte)l;
      l >>>= 8;
    }
    return(ret);
  }

  public static byte[] packRaw(byte[] b) throws IOException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    GZIPOutputStream zos = new GZIPOutputStream(baos);
    zos.write(b);
    zos.close();

    return baos.toByteArray();
  }

  public static byte[] unpackRaw(byte[] b) throws IOException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ByteArrayInputStream bais = new ByteArrayInputStream(b);

    GZIPInputStream zis = new GZIPInputStream(bais);
    byte[] tmpBuffer = new byte[256];
    int n;
    while ((n = zis.read(tmpBuffer)) >= 0) baos.write(tmpBuffer, 0, n);
    zis.close();

    return baos.toByteArray();
  }

  public static byte[] encryptRaw(byte[] key, byte[] b) throws IOException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (int i = 0; i < b.length; i++)
    {
      baos.write(b[i]^key[i%key.length]);
    }
    //baos.close();
    return(baos.toByteArray());
  }

  public static byte[] decryptRaw(byte[] key, byte[] b) throws IOException
  {
    return(encryptRaw(key, b));
  }
}
