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

// Title :        Const.java
// Version :      1.2
// Copyright :    Copyright (c) 2001-2002
// Author :       Florent CUETO  & Sebastien LEBRETON <socksviahttp@cqs.dyndns.org>
// Description :  Constants

package socksviahttp.core.consts;

public class Const
{
  // Class constants
  public static final String APPLICATION_VERSION = "1.2";
  public static final String APPLICATION_NAME = "Socks via HTTP";
  public static final String AUTHOR_NAME = "Florent CUETO & Sebastien LEBRETON";
  public static final String AUTHOR_EMAIL = "socksviahttp@cqs.dyndns.org";


  // Comm client <-> server
  public static final int CONNECTION_UNSPECIFIED_TYPE = 1;
  public static final int CONNECTION_NOT_FOUND = 2;
  public static final int CONNECTION_CREATE = 11;
  public static final int CONNECTION_CREATE_OK = 12;
  public static final int CONNECTION_CREATE_KO = 13;
  public static final int CONNECTION_PING = 21;
  public static final int CONNECTION_PONG = 22;
  public static final int CONNECTION_PONG_RECEIVED = 23;
  public static final int CONNECTION_VERSION_REQUEST = 24;
  public static final int CONNECTION_VERSION_RESPONSE_OK = 25;
  public static final int CONNECTION_VERSION_RESPONSE_KO = 26;
  public static final int CONNECTION_REQUEST = 31;
  public static final int CONNECTION_RESPONSE = 32;
  public static final int CONNECTION_DESTROY = 41;
  public static final int CONNECTION_DESTROY_OK = 42;
  public static final int CONNECTION_DESTROY_KO = 43;
  public static final int CONNECTION_WRONG_ENCRYPTION_KEY = 101;

  public static final byte[] TAB_EMPTY = "".getBytes();
}
