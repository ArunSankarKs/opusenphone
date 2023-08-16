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

// Title :        DataPacket.java
// Version :      1.2
// Copyright :    Copyright (c) 2001-2002
// Author :       Florent CUETO  & Sebastien LEBRETON <socksviahttp@cqs.dyndns.org>
// Description :  Data to transfer between the client part & the server part

package socksviahttp.core.net;

import java.io.*;
import java.util.zip.CRC32;

import socksviahttp.core.util.*;

public class DataPacket implements java.io.Serializable
{
  //public String login = "";
  //public String password = "";
  public int type = socksviahttp.core.consts.Const.CONNECTION_UNSPECIFIED_TYPE;
  public String id = "";
  public byte[] tab = socksviahttp.core.consts.Const.TAB_EMPTY;
  public boolean isConnClosed = false;
  public boolean zipData = false;
  public boolean encryptData = false;
  public byte[] encryptionKey = null;
  public int errorCode = 0;
  //public String version = "";

  /*    Message format:

        +-----+--------+----------+------------+----------+------------+------------+---------+------------+------+
        | TYPE|  Flags |  ID len  |  Conn ID   | Data len |    Data    | Data CRC32 | Ext len | EXTENSIONS | NULL |
        +-----+--------+----------+------------+----------+------------+------------+---------+------------+------+
        |  1  |    1   |     1    |  1 -> 255  |     2    | 0 -> 65535 |      8     |    2    |  variable  |  1   |
        +-----+--------+----------+------------+----------+------------+------------+---------+------------+------+


        TYPE = type
        Flags =
            0 : isConnClosed
            1 : zipData
            2 : encryptData
            3 : true if this packet is actually zipped
            4 : true if this packet is actually encrypted
        ID len = id.length
        Conn ID = id.getBytes()
        Data len = tab.length
        Data = tab
        Data CRC32 = CRC32 of tab (before zip & encryption)
        Ext len = 0 (for now)
        EXTENSIONS = Not used yet
        NULL = null byte (all bits set to 0)
  */

  public byte[] saveToByteArray()
  {
    boolean isThisPacketZipped = false;
    boolean isThisPacketEncrypted = false;
    byte[] workTab = tab;

    if (zipData)
    {
      try
      {
        byte[] zipTab = ByteUtils.packRaw(workTab);
        if (zipTab.length < workTab.length)
        {
          workTab = zipTab;
          isThisPacketZipped = true;
        }
        else isThisPacketZipped = false;
      }
      catch(IOException ioe)
      {
        isThisPacketZipped = false;
      }
    }
    if (encryptData)
    {
      try
      {
        //workTab = ByteUtils.encryptRaw("SOCKS_VIA_HTTP_ENCRYPTION_KEY".getBytes(), workTab);
        workTab = ByteUtils.encryptRaw(encryptionKey, workTab);
        isThisPacketEncrypted = true;
      }
      catch(IOException e)
      {
        isThisPacketEncrypted = false;
      }
    }

    byte[] extensions = new byte[0];

    byte[] array = new byte[1 + 1 + 1 + id.length() + 2 + workTab.length + 8 + 2 + extensions.length + 1];
    // Type
    array[0] = ByteUtils.i2b(type);
    // Flags
    array[1] = ByteUtils.byteFromBooleans(false, false, false, isThisPacketEncrypted, isThisPacketZipped, encryptData, zipData, isConnClosed);
    // Id len
    array[2] = ByteUtils.i2b(id.length());
    // Id
    byte[] tabId = id.getBytes();
    System.arraycopy(tabId, 0, array, 3, tabId.length);
    // Tab len
    array[tabId.length + 3] = ByteUtils.i2b(workTab.length / 256);
    array[tabId.length + 4] = ByteUtils.i2b(workTab.length % 256);
    // Tab
    System.arraycopy(workTab, 0, array, tabId.length + 5, workTab.length);
    // CRC
    java.util.zip.CRC32 crcComputer = new java.util.zip.CRC32();
    crcComputer.update(tab);
    long crc = crcComputer.getValue();
    byte[] bCrc = ByteUtils.bytesFromLong(crc);
    System.arraycopy(bCrc, 0, array, tabId.length + workTab.length + 5, 8);
    // Ext id
    array[tabId.length + workTab.length + 13] = ByteUtils.i2b(extensions.length / 256);
    array[tabId.length + workTab.length + 14] = ByteUtils.i2b(extensions.length % 256);
    // Extensions
    System.arraycopy(extensions, 0, array, tabId.length + workTab.length + 15, extensions.length);
    // Null
    array[tabId.length + workTab.length + extensions.length + 15] = 0;

    return(array);
  }

  public int loadFromByteArray(byte[] array)
  {
    type = ByteUtils.b2i(array[0]);
    boolean[] flags = ByteUtils.booleansFromByte(array[1]);
    isConnClosed = flags[0];
    zipData = flags[1];
    encryptData = flags[2];
    boolean isThisPacketZipped = flags[3];
    boolean isThisPacketEncrypted = flags[4];

    int idLen = ByteUtils.b2i(array[2]);
    id = new String(array, 3, idLen);

    int dataLen = 256 * ByteUtils.b2i(array[3 + idLen]) + ByteUtils.b2i(array[4 + idLen]);
    byte[] workTab = new byte[dataLen];
    System.arraycopy(array, 5 + idLen, workTab, 0, dataLen);

    // CRC
    long crc = ByteUtils.longFromBytes(array[12 + idLen + dataLen], array[11 + idLen + dataLen], array[10 + idLen + dataLen], array[9 + idLen + dataLen], array[8 + idLen + dataLen], array[7 + idLen + dataLen], array[6 + idLen + dataLen], array[5 + idLen + dataLen]);
    // Ext len
    int extLen = 256 * ByteUtils.b2i(array[13 + idLen + dataLen]) + ByteUtils.b2i(array[14 + idLen + dataLen]);
    // Extensions
    byte[] extensions = new byte[extLen];
    System.arraycopy(array, 15 + idLen + dataLen, extensions, 0, extLen);
    // Null
    byte nullByte = array[15 + idLen + dataLen + extLen];

    // Check null
    if (nullByte != 0)
    {
      // Log a warning
      // TO DO
    }

    // Data extraction
    if (isThisPacketEncrypted)
    {
      try
      {
        //workTab = ByteUtils.decryptRaw("SOCKS_VIA_HTTP_ENCRYPTION_KEY".getBytes(), workTab);
        workTab = ByteUtils.decryptRaw(encryptionKey, workTab);
      }
      catch(IOException e)
      {
        tab = workTab;
        errorCode = 3;
        return(3);
      }
    }
    if (isThisPacketZipped)
    {
      try
      {
        workTab = ByteUtils.unpackRaw(workTab);
      }
      catch(IOException ioe)
      {
        tab = workTab;
        errorCode = 2;
        return(2);
      }
    }
    tab = workTab;

    // Check CRC
    CRC32 crcComputer = new CRC32();
    crcComputer.update(tab,0, tab.length);
    long computedCrc = crcComputer.getValue();

    if (computedCrc != crc)
    {
      errorCode = 1;
      return(1);
    }

    // Return
    errorCode = 0;
    return(0);
  }
}
