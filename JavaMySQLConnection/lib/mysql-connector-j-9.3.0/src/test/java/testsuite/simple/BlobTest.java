/*
 * Copyright (c) 2002, 2025, Oracle and/or its affiliates.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License, version 2.0, as published by
 * the Free Software Foundation.
 *
 * This program is designed to work with certain software that is licensed under separate terms, as designated in a particular file or component or in
 * included license documentation. The authors of MySQL hereby grant you an additional permission to link the program and your derivative works with the
 * separately licensed software that they have either included with the program or referenced in the documentation.
 *
 * Without limiting anything contained in the foregoing, this file, which is part of MySQL Connector/J, is also subject to the Universal FOSS Exception,
 * version 1.0, a copy of which can be found at http://oss.oracle.com/licenses/universal-foss-exception.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License, version 2.0, for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package testsuite.simple;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import testsuite.BaseTestCase;

/**
 * Tests BLOB functionality in the driver.
 */
public class BlobTest extends BaseTestCase {

    protected static File testBlobFile;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                for (int i = 0; i < 5; i++) {
                    try {
                        if (testBlobFile.delete()) {
                            break;
                        }
                    } catch (Throwable t) {
                    }
                }
            }

        });
    }

    /**
     * Setup the test case
     *
     * @throws Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        int requiredSize = 32 * 1024 * 1024;

        if (testBlobFile == null || testBlobFile.length() != requiredSize) {
            createBlobFile(requiredSize);
        }

        createTestTable();
    }

    @Test
    public void testByteStreamInsert() throws Exception {
        this.rs = this.stmt.executeQuery("SHOW VARIABLES LIKE 'max_allowed_packet'");
        this.rs.next();
        long len = 4 + testBlobFile.length() * 2;
        assumeTrue(this.rs.getInt(2) >= len, "You need to increase max_allowed_packet to at least " + len + " before running this test!");

        if (versionMeetsMinimum(5, 6, 20) && !versionMeetsMinimum(5, 7)) {
            /*
             * The 5.6.20 patch for Bug #16963396, Bug #19030353, Bug #69477 limits the size of redo log BLOB writes
             * to 10% of the redo log file size. The 5.7.5 patch addresses the bug without imposing a limitation.
             * As a result of the redo log BLOB write limit introduced for MySQL 5.6, innodb_log_file_size should be set to a value
             * greater than 10 times the largest BLOB data size found in the rows of your tables plus the length of other variable length
             * fields (VARCHAR, VARBINARY, and TEXT type fields).
             */
            this.rs = this.stmt.executeQuery("SHOW VARIABLES LIKE 'innodb_log_file_size'");
            this.rs.next();
            assumeFalse(this.rs.getInt(2) < 10 * testBlobFile.length(),
                    "You need to increase innodb_log_file_size to at least " + 10 * testBlobFile.length() + " before running this test!");
        }
        testByteStreamInsert(this.conn);
    }

    /**
     * Tests inserting blob data as a stream
     *
     * @param c
     *
     * @throws Exception
     */
    private void testByteStreamInsert(Connection c) throws Exception {
        BufferedInputStream bIn = new BufferedInputStream(new FileInputStream(testBlobFile));
        this.pstmt = c.prepareStatement("INSERT INTO BLOBTEST(blobdata) VALUES (?)");
        this.pstmt.setBinaryStream(1, bIn, (int) testBlobFile.length());
        this.pstmt.execute();

        this.pstmt.clearParameters();
        doRetrieval();
    }

    private boolean checkBlob(byte[] retrBytes) throws Exception {
        boolean passed = false;
        BufferedInputStream bIn = new BufferedInputStream(new FileInputStream(testBlobFile));

        try {
            int fileLength = (int) testBlobFile.length();
            if (retrBytes.length == fileLength) {
                for (int i = 0; i < fileLength; i++) {
                    byte fromFile = (byte) (bIn.read() & 0xff);

                    if (retrBytes[i] != fromFile) {
                        passed = false;
                        System.out.println("Byte pattern differed at position " + i + " , " + retrBytes[i] + " != " + fromFile);

                        for (int j = 0; j < i + 10 /* && (j < i) */; j++) {
                            System.out.print(Integer.toHexString(retrBytes[j] & 0xff) + " ");
                        }

                        break;
                    }

                    passed = true;
                }
            } else {
                passed = false;
                System.out.println("retrBytes.length(" + retrBytes.length + ") != testBlob.length(" + fileLength + ")");
            }

            return passed;
        } finally {
            if (bIn != null) {
                bIn.close();
            }
        }
    }

    private void createTestTable() throws Exception {
        createTable("BLOBTEST", "(pos int PRIMARY KEY auto_increment, blobdata LONGBLOB)");
    }

    /**
     * Mark this as deprecated to avoid warnings from compiler...
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    private void doRetrieval() throws Exception {
        boolean passed = false;
        this.rs = this.stmt.executeQuery("SELECT blobdata from BLOBTEST LIMIT 1");
        this.rs.next();

        byte[] retrBytes = this.rs.getBytes(1);
        passed = checkBlob(retrBytes);
        assertTrue(passed, "Inserted BLOB data did not match retrieved BLOB data for getBytes().");
        retrBytes = this.rs.getBlob(1).getBytes(1L, (int) this.rs.getBlob(1).length());
        passed = checkBlob(retrBytes);
        assertTrue(passed, "Inserted BLOB data did not match retrieved BLOB data for getBlob().");

        InputStream inStr = this.rs.getBinaryStream(1);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        int b;

        while ((b = inStr.read()) != -1) {
            bOut.write((byte) b);
        }

        retrBytes = bOut.toByteArray();
        passed = checkBlob(retrBytes);
        assertTrue(passed, "Inserted BLOB data did not match retrieved BLOB data for getBinaryStream().");
        inStr = this.rs.getAsciiStream(1);
        bOut = new ByteArrayOutputStream();

        while ((b = inStr.read()) != -1) {
            bOut.write((byte) b);
        }

        retrBytes = bOut.toByteArray();
        passed = checkBlob(retrBytes);
        assertTrue(passed, "Inserted BLOB data did not match retrieved BLOB data for getAsciiStream().");
        inStr = this.rs.getUnicodeStream(1);
        bOut = new ByteArrayOutputStream();

        while ((b = inStr.read()) != -1) {
            bOut.write((byte) b);
        }

        retrBytes = bOut.toByteArray();
        passed = checkBlob(retrBytes);
        assertTrue(passed, "Inserted BLOB data did not match retrieved BLOB data for getUnicodeStream().");
    }

    private final static String TEST_BLOB_FILE_PREFIX = "cmj-testblob";

    private void createBlobFile(int size) throws Exception {
        if (testBlobFile != null && testBlobFile.length() != size) {
            testBlobFile.delete();
        }

        testBlobFile = File.createTempFile(TEST_BLOB_FILE_PREFIX, ".dat");
        testBlobFile.deleteOnExit();

        // TODO: following cleanup doesn't work correctly during concurrent execution of testsuite
        // cleanupTempFiles(testBlobFile, TEST_BLOB_FILE_PREFIX);

        BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(testBlobFile));

        int dataRange = Byte.MAX_VALUE - Byte.MIN_VALUE;

        for (int i = 0; i < size; i++) {
            bOut.write((byte) (Math.random() * dataRange + Byte.MIN_VALUE));
        }

        bOut.flush();
        bOut.close();
    }

}
