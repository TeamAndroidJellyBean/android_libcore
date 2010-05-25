/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.luni.util;

import java.io.ByteArrayOutputStream;
import java.io.UTFDataFormatException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.TimeZone;

public final class Util {

    private static final String defaultEncoding;

    static {
        // BEGIN android-changed
        String encoding = System.getProperty("file.encoding");
        // END android-changed
        if (encoding != null) {
            try {
                "".getBytes(encoding);
            } catch (Throwable t) {
                encoding = null;
            }
        }
        defaultEncoding = encoding;
    }

    public static String toString(byte[] bytes) {
        if (defaultEncoding != null) {
            try {
                return new String(bytes, 0, bytes.length, defaultEncoding);
            } catch (java.io.UnsupportedEncodingException e) {
            }
        }
        return new String(bytes, 0, bytes.length);
    }

    public static String toUTF8String(byte[] bytes) {
        return toUTF8String(bytes, 0, bytes.length);
    }

    public static String toString(byte[] bytes, int offset, int length) {
        if (defaultEncoding != null) {
            try {
                return new String(bytes, offset, length, defaultEncoding);
            } catch (java.io.UnsupportedEncodingException e) {
            }
        }
        return new String(bytes, offset, length);
    }

    public static String toUTF8String(byte[] bytes, int offset, int length) {
        try {
            return new String(bytes, offset, length, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return toString(bytes, offset, length);
        }
    }

    public static String convertFromUTF8(byte[] buf, int offset, int utfSize) throws UTFDataFormatException {
        return convertUTF8WithBuf(buf, new char[utfSize], offset, utfSize);
    }

    public static String convertUTF8WithBuf(byte[] buf, char[] out, int offset, int utfSize) throws UTFDataFormatException {
        int count = 0, s = 0, a;
        while (count < utfSize) {
            if ((out[s] = (char) buf[offset + count++]) < '\u0080')
                s++;
            else if (((a = out[s]) & 0xe0) == 0xc0) {
                if (count >= utfSize) {
                    throw new UTFDataFormatException("Second byte at " + count + " does not match UTF8 Specification");
                }
                // BEGIN android-changed
                int b = buf[offset + count++];
                // END android-changed
                if ((b & 0xC0) != 0x80) {
                    throw new UTFDataFormatException("Second byte at " + (count - 1) + " does not match UTF8 Specification");
                }
                out[s++] = (char) (((a & 0x1F) << 6) | (b & 0x3F));
            } else if ((a & 0xf0) == 0xe0) {
                if (count + 1 >= utfSize) {
                    throw new UTFDataFormatException("Third byte at " + (count + 1) + " does not match UTF8 Specification");
                }
                // BEGIN android-changed
                int b = buf[offset + count++];
                int c = buf[offset + count++];
                // END android-changed
                if (((b & 0xC0) != 0x80) || ((c & 0xC0) != 0x80)) {
                    throw new UTFDataFormatException("Second or third byte at " + (count - 2) + " does not match UTF8 Specification");
                }
                out[s++] = (char) (((a & 0x0F) << 12) | ((b & 0x3F) << 6) | (c & 0x3F));
            } else {
                throw new UTFDataFormatException("Input at " + (count - 1) + " does not match UTF8 Specification");
            }
        }
        return new String(out, 0, s);
    }

    /**
     * '%' and two following hex digit characters are converted to the
     * equivalent byte value. All other characters are passed through
     * unmodified. e.g. "ABC %24%25" -> "ABC $%"
     *
     * @param s
     *            java.lang.String The encoded string.
     * @return java.lang.String The decoded version.
     */
    public static String decode(String s, boolean convertPlus) {
        return decode(s, convertPlus, null);
    }

    /**
     * '%' and two following hex digit characters are converted to the
     * equivalent byte value. All other characters are passed through
     * unmodified. e.g. "ABC %24%25" -> "ABC $%"
     *
     * @param s
     *            java.lang.String The encoded string.
     * @param encoding
     *            the specified encoding
     * @return java.lang.String The decoded version.
     */
    public static String decode(String s, boolean convertPlus, String encoding) {
        if (!convertPlus && s.indexOf('%') == -1)
            return s;
        StringBuilder result = new StringBuilder(s.length());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < s.length();) {
            char c = s.charAt(i);
            if (convertPlus && c == '+')
                result.append(' ');
            else if (c == '%') {
                out.reset();
                do {
                    if (i + 2 >= s.length()) {
                        throw new IllegalArgumentException("Incomplete % sequence at: " + i);
                    }
                    int d1 = Character.digit(s.charAt(i + 1), 16);
                    int d2 = Character.digit(s.charAt(i + 2), 16);
                    if (d1 == -1 || d2 == -1) {
                        throw new IllegalArgumentException("Invalid % sequence " +
                                s.substring(i, i + 3) + " at " + i);
                    }
                    out.write((byte) ((d1 << 4) + d2));
                    i += 3;
                } while (i < s.length() && s.charAt(i) == '%');
                if (encoding == null) {
                    result.append(out.toString());
                } else {
                    try {
                        result.append(out.toString(encoding));
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
                continue;
            } else
                result.append(c);
            i++;
        }
        return result.toString();
    }

    public static String toASCIILowerCase(String s) {
        int len = s.length();
        StringBuilder buffer = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if ('A' <= c && c <= 'Z') {
                buffer.append((char) (c + ('a' - 'A')));
            } else {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }

    public static String toASCIIUpperCase(String s) {
        int len = s.length();
        StringBuilder buffer = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if ('a' <= c && c <= 'z') {
                buffer.append((char) (c - ('a' - 'A')));
            } else {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }
}
