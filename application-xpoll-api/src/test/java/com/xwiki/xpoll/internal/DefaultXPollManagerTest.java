/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.xpoll.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiRequest;
import com.xwiki.xpoll.PollResultsCalculator;
import com.xwiki.xpoll.XPollException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultXPollManager}.
 *
 * @version $Id$
 * @since 2.1
 */
@ComponentTest
class DefaultXPollManagerTest
{
    @InjectMockComponents
    private DefaultXPollManager manager;

    @InjectMockComponents
    private CondorcetPollResultsCalculator calculator;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @MockComponent
    private ComponentManager componentManager;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private XWikiRequest request;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument document;

    @Mock
    private BaseObject xpollObj;

    @BeforeEach
    void setup() throws ComponentLookupException
    {
        when(this.contextProvider.get()).thenReturn(this.xWikiContext);
        when(this.componentManagerProvider.get()).thenReturn(this.componentManager);
        when(this.componentManager.getInstance(PollResultsCalculator.class, "condorcet")).thenReturn(this.calculator);
    }

    @Test
    void urlMultipleSpaces()
    {
        when(this.xWikiContext.getRequest()).thenReturn(this.request);
        when(this.request.getContextPath()).thenReturn("xwiki");

        DocumentReference docRef = new DocumentReference("XWiki", Arrays.asList("Space1", "Space2"), "Page");

        String result = manager.getRestURL(docRef);
        String expected = "xwiki/rest/wikis/XWiki/spaces/Space1/spaces/Space2/pages/Page/xpoll";
        assertEquals(expected, result);
    }

    @Test
    void getVoteResultsWithNoVotes() throws XWikiException, XPollException
    {
        DocumentReference docRef = new DocumentReference("XWiki", Arrays.asList("Space1", "Space2"), "Page");

        when(this.xWikiContext.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getDocument(docRef, this.xWikiContext)).thenReturn(this.document);
        when(this.document.getXObject(DefaultXPollManager.XPOLL_CLASS_REFERENCE)).thenReturn(this.xpollObj);
        when(this.xpollObj.getListValue(DefaultXPollManager.PROPOSALS)).thenReturn(Arrays.asList("Proposal1",
            "Proposal2", "Proposal3"));
        when(this.document.getXObjects(DefaultXPollManager.XPOLL_VOTES_CLASS_REFERENCE)).thenReturn(new ArrayList<>());
        when(this.xpollObj.getStringValue(DefaultXPollManager.XPOLL_TYPE)).thenReturn("condorcet");

        Map<String, Integer> results = manager.getVoteResults(docRef);

        assertEquals(3, results.size());
        assertEquals(new HashSet<>(Collections.singletonList(2)), new HashSet<>(results.values()));
    }

    @Test
    void getVoteResultsWithCondorcetType() throws XWikiException, XPollException
    {
        DocumentReference docRef = new DocumentReference("XWiki", Arrays.asList("Space1", "Space2"), "Page");
        when(this.xWikiContext.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getDocument(docRef, this.xWikiContext)).thenReturn(this.document);
        when(this.document.getXObject(DefaultXPollManager.XPOLL_CLASS_REFERENCE)).thenReturn(this.xpollObj);
        when(this.xpollObj.getStringValue(DefaultXPollManager.XPOLL_TYPE)).thenReturn("condorcet");

        String proposal1 = "Proposal1";
        String proposal2 = "Proposal2";
        String proposal3 = "Proposal3";

        when(this.xpollObj.getListValue(DefaultXPollManager.PROPOSALS)).thenReturn(Arrays.asList(proposal1,
            proposal2, proposal3));

        BaseObject ballot1 = mock(BaseObject.class);
        BaseObject ballot2 = mock(BaseObject.class);
        BaseObject ballot3 = mock(BaseObject.class);
        BaseObject ballot4 = mock(BaseObject.class);
        BaseObject ballot5 = mock(BaseObject.class);

        when(this.document.getXObjects(DefaultXPollManager.XPOLL_VOTES_CLASS_REFERENCE))
            .thenReturn(Arrays.asList(ballot1, ballot2, ballot3, ballot4, ballot5));

        when(ballot1.getListValue(DefaultXPollManager.VOTES))
            .thenReturn(Arrays.asList(proposal1, proposal3, proposal2));
        when(ballot2.getListValue(DefaultXPollManager.VOTES))
            .thenReturn(Arrays.asList(proposal2, proposal1, proposal3));
        when(ballot3.getListValue(DefaultXPollManager.VOTES))
            .thenReturn(Arrays.asList(proposal2, proposal1, proposal3));
        when(ballot4.getListValue(DefaultXPollManager.VOTES))
            .thenReturn(Arrays.asList(proposal2, proposal1, proposal3));
        when(ballot5.getListValue(DefaultXPollManager.VOTES))
            .thenReturn(Arrays.asList(proposal3, proposal1, proposal2));

        Map<String, Integer> results = manager.getVoteResults(docRef);

        assertEquals(1, results.get(proposal1));
        assertEquals(2, results.get(proposal2));
        assertEquals(0, results.get(proposal3));
    }
}
