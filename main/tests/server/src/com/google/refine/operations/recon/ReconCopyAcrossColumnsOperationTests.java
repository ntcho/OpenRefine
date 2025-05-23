/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.operations.recon;

import static org.testng.Assert.assertEquals;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.node.TextNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.RefineTest;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.operations.OperationDescription;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ReconCopyAcrossColumnsOperationTests extends RefineTest {

    String json = "{\"op\":\"core/recon-copy-across-columns\","
            + "\"description\":"
            + new TextNode(OperationDescription.recon_copy_across_columns_brief("source column", "first, second")).toString() + ","
            + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
            + "\"fromColumnName\":\"source column\","
            + "\"toColumnNames\":[\"first\",\"second\"],"
            + "\"judgments\":[\"matched\",\"new\"],"
            + "\"applyToJudgedCells\":true}";
    Project project;

    @BeforeSuite
    public void registerOperation() {
        OperationRegistry.registerOperation(getCoreModule(), "recon-copy-across-columns", ReconCopyAcrossColumnsOperation.class);
    }

    @BeforeMethod
    public void setupInitialState() {
        project = createProject(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { "d", new Cell("b", testRecon("e", "h", Recon.Judgment.Matched)) },
                        { "b", new Cell("d", testRecon("b", "j", Recon.Judgment.None)) }
                });
    }

    @Test
    public void serializeReconCopyAcrossColumnsOperation() throws Exception {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ReconCopyAcrossColumnsOperation.class), json);
    }

    @Test
    public void testColumnDependencies() throws Exception {
        AbstractOperation op = ParsingUtilities.mapper.readValue(json, ReconCopyAcrossColumnsOperation.class);
        assertEquals(op.getColumnsDiff(), Optional.of(ColumnsDiff.builder().modifyColumn("first").modifyColumn("second").build()));
        assertEquals(op.getColumnDependencies(), Optional.of(Set.of("source column", "first", "second")));
    }

    @Test
    public void testReconCopyAcrossColumns() throws Exception {
        AbstractOperation operation = new ReconCopyAcrossColumnsOperation(
                new EngineConfig(Collections.emptyList(), Mode.RowBased),
                "bar",
                new String[] { "foo" },
                new String[] { "matched", "none" },
                true);

        runOperation(operation, project);

        Project expected = createProject(
                new String[] { "foo", "bar" },
                new Serializable[][] {
                        { "a", "b" },
                        { new Cell("d", testRecon("b", "j", Recon.Judgment.None)),
                                new Cell("b", testRecon("e", "h", Recon.Judgment.Matched)) },
                        { new Cell("b", testRecon("e", "h", Recon.Judgment.Matched)),
                                new Cell("d", testRecon("b", "j", Recon.Judgment.None)) }
                });

        assertProjectEquals(project, expected);
    }

    @Test
    public void testRename() throws Exception {
        AbstractOperation operation = new ReconCopyAcrossColumnsOperation(
                new EngineConfig(Collections.emptyList(), Mode.RowBased),
                "bar",
                new String[] { "foo" },
                new String[] { "matched", "none" },
                true);

        AbstractOperation renamed = operation.renameColumns(Map.of("bar", "bar2", "foo", "foo2", "none", "what?"));

        String expectedJson = "{\"op\":\"core/recon-copy-across-columns\","
                + "\"description\":"
                + new TextNode(OperationDescription.recon_copy_across_columns_brief("bar2", "foo2")).toString() + ","
                + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
                + "\"fromColumnName\":\"bar2\","
                + "\"toColumnNames\":[\"foo2\"],"
                + "\"judgments\":[\"matched\",\"none\"],"
                + "\"applyToJudgedCells\":true}";
        TestUtils.isSerializedTo(renamed, expectedJson);
    }

}
