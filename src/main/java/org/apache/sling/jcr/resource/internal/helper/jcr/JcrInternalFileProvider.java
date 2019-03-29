/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.resource.internal.helper.jcr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.oak.api.blob.FileReferencable;
import org.apache.jackrabbit.oak.api.blob.TempFileReference;

import org.apache.sling.api.resource.InternalFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is class implements InternalFileProvider interface for a JCR resource that can be represented as file such as JCR 
 * node of type nt:file or node with jcr:data property.
 * 
 * @author hsaginor
 *
 */
public class JcrInternalFileProvider implements InternalFileProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger( JcrInternalFileProvider.class );

    private Binary data;
    private TempFileReference fileRef;
    private File tmpFile;

    JcrInternalFileProvider(Binary data) {
        this.data = data;
    }

    @Override
    public File getFile() {
        if (tmpFile == null) {
            String fileName = "sling-jcr-file-" + System.currentTimeMillis();

            try {
                tmpFile = getOAKFile(fileName);
            } catch (RepositoryException e1) {
                LOGGER.warn("Unable to get tempruary file from OAK.", e1);
            } catch (IOException e1) {
                LOGGER.warn("Unable to get tempruary file from OAK.", e1);
            }

            if (tmpFile != null) {
                // OAK provided a file reference.
                // Nothing else to do here.
                return tmpFile;
            }

            FileOutputStream out = null;
            InputStream in = null;

            try {

                tmpFile = File.createTempFile(fileName, null);
                tmpFile.deleteOnExit();

                out = new FileOutputStream(tmpFile);
                in = data.getStream();
                IOUtils.copy(in, out);
            } catch (IOException e) {
                LOGGER.error("Unable to create tempruary file.", e);
            } catch (RepositoryException e) {
                LOGGER.error("Unable to create tempruary file.", e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                    }
                }

                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        return tmpFile;
    }

    @Override
    public void release() {
        if (fileRef != null) {
            fileRef.close();
            tmpFile = null;
            fileRef = null;
        }

        if (tmpFile != null) {
            tmpFile.delete();
            tmpFile = null;
        }
    }

    private File getOAKFile(String nameHint) throws RepositoryException, IOException {
        if (data instanceof FileReferencable) {
            if (fileRef == null) {
                fileRef = ((FileReferencable) data).getTempFileReference();
            }

            return fileRef.getTempFile(nameHint, null);
        }
        return null;
    }

}
