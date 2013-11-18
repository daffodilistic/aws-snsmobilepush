/*
 * Copyright 2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.kindletest;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import android.util.Base64;

/**
 * This class provides a method to calculate the checksum on a
 * collection of key-value pairs by the algorithm defined in the
 * ADM documentation.
 *
 * @version Revision: 1, Date: 11/11/2012
 */
public class ADMSampleMD5ChecksumCalculator
{
    /** The byte encoding used in the MD5 computation. */
    private static final String ENCODING = "UTF-8";

    /** The algorithm used in the MD5 computation. */
    private static final String ALGORITHM = "MD5";

    /**
     * Calculates the md5 checksum of the provided collection of
     * key-value pairs according to the algorithm defined in the
     * ADM documentation:
     * 1. Sort the key-value pairs using a UTF-8 code unit-based comparison
     *    of the keys.
     * 2. Concatenate the series of pairs in the format:
     *     a. "key1:value1,key2:value2"
     *     b. There should be no whitespace between the ':' character and
     *        either the keys or values. There should also be no whitespace
     *        in between each pair and the ',' character.
     * 3. Compute the md5 using the UTF-8 bytes of the string produced in
     *    step 2 according to the algorithm defined in RFC 1321.
     * 4. Base-64 encode the 128-bit output of the md5 algorithm that was
     *    computed in step 3.
     *
     * @param input The input to compute the MD5 checksum on.
     * @return The MD5 checksum of the input.
     */
    public String calculateChecksum(final Map<String, String> input)
    {
        final String serializedMapData = getSerializedMap(input);
        final byte[] md5Bytes = getMd5Bytes(serializedMapData);
        final String base64Digest = new String(Base64.encode(md5Bytes, Base64.DEFAULT));
        return base64Digest;
    }
    
    /**
     * This method sorts the collection of key-value pairs by a UTF-8
     * code unit comparison, and serializes the result.
     * 
     * @param input The input to serialize.
     * @return The serialized version of the input.
     */
    private String getSerializedMap(final Map<String,String> input)
    {
        final SortedMap<String, String> sortedMap = 
                new TreeMap<String,String>(new UTF8CodeUnitStringComparator());
        sortedMap.putAll(input);
        
        final StringBuilder builder = new StringBuilder();
        int numElements = 1;
        for (final Entry<String, String> entry : sortedMap.entrySet()) {
            builder.append(String.format("%s:%s", entry.getKey(), entry.getValue()));
            if (numElements++ < sortedMap.size()) {
                builder.append(",");
            }
        }
        return builder.toString();
    }
    
    /**
     * Generates an md5 using the UTF-8 bytes of a string.
     * 
     * @param input The string used to generate the md5 digest.
     * @return The bytes of the md5 result.
     */
    private byte[] getMd5Bytes(final String input)
    {
        final byte[] serializedBytes;
        try {
            serializedBytes = input.getBytes(ENCODING);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(ENCODING + " not supported!", e);
        }
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(ALGORITHM);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(ALGORITHM + " not supported!", e);
        }
        digest.update(serializedBytes);
        return digest.digest();
    }
    
    /**
     * An implementation of {@link Comparator<String>} that performs a UTF-8
     * code unit-based comparison on two strings.
     */
    private class UTF8CodeUnitStringComparator implements Comparator<String>, Serializable
    {
        /** {@inheritDoc} */
        public int compare(final String str1, final String str2)
        {
            try {
                // Retrieve the UTF-8 code units from the two strings.
                final byte[] bytes1 = str1.getBytes(ENCODING);
                final byte[] bytes2 = str2.getBytes(ENCODING);
                // Calculate the loop bounds so that we do not exceed the length of either string.
                final int loopBounds = Math.min(bytes1.length, bytes2.length);
                for(int i = 0; i < loopBounds; i++) {
                    // We want the unsigned byte values for comparison.
                    final int ub1 = bytes1[i] & 0xFF;
                    final int ub2 = bytes2[i] & 0xFF;
                    // If the two code units are not equivalent, return the difference.
                    if(ub1 != ub2) {
                        return ub1 - ub2;
                    }
                }
                // If we have reached this point, one string is a substring
                // of the other, and we return the difference in their lengths.
                // If the two strings are equivalent, this difference will be 0.
                return bytes1.length - bytes2.length;
            } catch (final UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 not supported!", e);
            }
        }
    }
}
