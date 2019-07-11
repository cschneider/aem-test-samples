/*
 * Copyright 2018 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.cq.testing.it.smoke;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.Page;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.util.URLParameterBuilder;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.junit.*;

import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;

public class SearchIT {

    private static final long TIMEOUT = SECONDS.toMillis(30);
    private static final String OMNISEARCH_PATH = "/mnt/overlay/granite/ui/content/shell/omnisearch/searchresults.html";
    private static final String SEARCH_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit";

    @ClassRule
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    @Rule
    public Page root = new Page(cqBaseClassRule.authorRule);

    static CQClient adminAuthor;


    static String damParentPath = "/content/dam/test-" + System.currentTimeMillis();
    
    static String imgName = "testPicture.png";
    static String imgResourcePath = "/com/adobe/cq/testing/it/smoke/picture/" + imgName;
    static String imgMimeType = "image/png";

    static String pdfName = "testPDF.pdf";
    static String pdfResourcePath = "/com/adobe/cq/testing/it/smoke/pdf/" + pdfName;
    static String pdfMimeType = "application/pdf";


    @BeforeClass
    public static void beforeClass() throws ClientException {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        //upload one test picture
        adminAuthor.uploadAsset(imgName, imgResourcePath, imgMimeType, damParentPath, SC_CREATED, SC_OK);

        //upload one test pdf
        adminAuthor.uploadAsset(pdfName, pdfResourcePath, pdfMimeType, damParentPath, SC_CREATED, SC_OK);
    }

    @AfterClass
    public static void afterClass() throws ClientException {
        adminAuthor.deletePath(damParentPath);
    }

    /**
     * Utility that uses omnisearch with a pooler to search using a {{searchText}} in a specific {{location}}
     * and checks that the expected {{expectedResult}} is in the response
     */
    private void checkSearchResults(final String searchText, final String location, final String expectedResult) throws TimeoutException, InterruptedException {

        new Polling() {
            @Override
            public Boolean call() throws Exception {
                return adminAuthor.doGet(OMNISEARCH_PATH, URLParameterBuilder.create()
                                .add("fulltext", searchText)
                                .add("location", location)
                                .getList(),
                        SC_OK).getContent().toString().contains(expectedResult);
            }
        }.poll(TIMEOUT, 500);
    }

    /**
     * Verifies that searching for the text inside the page created by the Page rule
     * returns the page containing it
     */
    @Test
    public void searchInPages() throws Exception {

        new Polling() {
            @Override
            public Boolean call() throws Exception {
                return adminAuthor.searchInPages(root.getPath(), "This page was generated by an automated integration test", false, false)
                        .toString().contains(root.getPath());
            }
        }.poll(TIMEOUT, 500);
    }

    /**
     * Uses omnisearch to search for the page created by the Page rule
     */
    @Test
    public void searchSites() throws Exception {

        checkSearchResults(root.getName(), "site", root.getName());
    }

    /**
     * Uses omnisearch to search for a picture uploaded during test setup
     */
    @Test
    public void searchUploadedAssets() throws Exception {

        checkSearchResults(imgName, "asset", imgName);
    }

    /**
     * Uses omnisearch to search for text inside PDF uploaded during test setup
     */
    @Test
    @Ignore("CQ-4274795: Text extraction issue, re-enable when nui asset processing is enabled")
    public void searchTextInPdf() throws Exception {

        checkSearchResults(SEARCH_TEXT, "asset", pdfName);
    }
}