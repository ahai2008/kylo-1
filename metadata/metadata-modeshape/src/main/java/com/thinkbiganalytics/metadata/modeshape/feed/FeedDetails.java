/**
 * 
 */
package com.thinkbiganalytics.metadata.modeshape.feed;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import com.thinkbiganalytics.metadata.api.category.Category;
import com.thinkbiganalytics.metadata.api.datasource.Datasource;
import com.thinkbiganalytics.metadata.api.feed.Feed;
import com.thinkbiganalytics.metadata.api.feed.FeedDestination;
import com.thinkbiganalytics.metadata.api.feed.FeedPrecondition;
import com.thinkbiganalytics.metadata.api.feed.FeedSource;
import com.thinkbiganalytics.metadata.api.feedmgr.template.FeedManagerTemplate;
import com.thinkbiganalytics.metadata.modeshape.MetadataRepositoryException;
import com.thinkbiganalytics.metadata.modeshape.common.JcrPropertiesEntity;
import com.thinkbiganalytics.metadata.modeshape.datasource.JcrDatasource;
import com.thinkbiganalytics.metadata.modeshape.sla.JcrServiceLevelAgreement;
import com.thinkbiganalytics.metadata.modeshape.support.JcrPropertyUtil;
import com.thinkbiganalytics.metadata.modeshape.support.JcrUtil;
import com.thinkbiganalytics.metadata.modeshape.support.JcrVersionUtil;
import com.thinkbiganalytics.metadata.modeshape.template.JcrFeedTemplate;
import com.thinkbiganalytics.metadata.sla.api.ServiceLevelAgreement;

/**
 *
 */
public class FeedDetails<C extends Category> extends JcrPropertiesEntity {

    public static final String FEED_JSON = "tba:json";
    public static final String PROCESS_GROUP_ID = "tba:processGroupId";
    public static final String FEED_TEMPLATE = "tba:feedTemplate";

    public static final String PRECONDITION = "tba:precondition";
    public static final String DEPENDENTS = "tba:dependentFeeds";
    public static final String USED_BY_FEEDS = "tba:usedByFeeds";
    public static final String SOURCE_NAME = "tba:sources";
    public static final String DESTINATION_NAME = "tba:destinations";

    public static final String TEMPLATE = "tba:template";
    public static final String SLA = "tba:slas";

    /**
     * @param node
     */
    public FeedDetails(Node node) {
        super(node);
    }

    protected JcrFeed<C> getParentFeed() {
        try {
            return new JcrFeed(this.node.getParent().getParent());
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to retrieve the parent feed of the feed details", e);
        }
    }
    
    protected FeedSummary getParentSummary() {
        try {
            return new FeedSummary(this.node.getParent());
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to retrieve the parent feed summary of the feed details", e);
        }
    }

    public List<? extends FeedSource> getSources() {
        return JcrUtil.getJcrObjects(this.node, SOURCE_NAME, JcrFeedSource.class);
    }

    public List<? extends FeedDestination> getDestinations() {
        return JcrUtil.getJcrObjects(this.node, DESTINATION_NAME, JcrFeedDestination.class);
    }

    public <C extends Category> List<Feed<C>> getDependentFeeds() {
        List<Feed<C>> deps = new ArrayList<>();
        Set<Node> depNodes = JcrPropertyUtil.getSetProperty(this.node, DEPENDENTS);

        for (Node depNode : depNodes) {
            deps.add(new JcrFeed<C>(depNode));
        }

        return deps;
    }

    public boolean addDependentFeed(Feed<?> feed) {
        JcrFeed<?> dependent = (JcrFeed<?>) feed;
        Node depNode = dependent.getNode();
        feed.addUsedByFeed(getParentFeed());

        return JcrPropertyUtil.addToSetProperty(this.node, DEPENDENTS, depNode);
    }
    
    public boolean removeDependentFeed(Feed<?> feed) {
        JcrFeed<?> dependent = (JcrFeed<?>) feed;
        Node depNode = dependent.getNode();
        feed.removeUsedByFeed(getParentFeed());
        return JcrPropertyUtil.removeFromSetProperty(this.node, DEPENDENTS, depNode);
    }

    public boolean addUsedByFeed(Feed<?> feed) {
        JcrFeed<?> dependent = (JcrFeed<?>) feed;
        Node depNode = dependent.getNode();

        return JcrPropertyUtil.addToSetProperty(this.node, USED_BY_FEEDS, depNode);
    }

    public List<Feed<C>> getUsedByFeeds() {
        List<Feed<C>> deps = new ArrayList<>();
        Set<Node> depNodes = JcrPropertyUtil.getSetProperty(this.node, USED_BY_FEEDS);

        for (Node depNode : depNodes) {
            deps.add(new JcrFeed<C>(depNode));
        }

        return deps;
    }

    public boolean removeUsedByFeed(Feed<?> feed) {
        JcrFeed<?> dependent = (JcrFeed<?>) feed;
        Node depNode = dependent.getNode();

        return JcrPropertyUtil.removeFromSetProperty(this.node, USED_BY_FEEDS, depNode);
    }

    public FeedSource getSource(final Datasource.ID id) {
        List<? extends FeedSource> sources = getSources();
        if (sources != null) {
            return sources.stream().filter(feedSource -> feedSource.getDatasource().getId().equals(id)).findFirst().orElse(null);
        }
        return null;
    }

    public FeedDestination getDestination(final Datasource.ID id) {
        List<? extends FeedDestination> destinations = getDestinations();
        if (destinations != null) {
            return destinations.stream().filter(feedDestination -> feedDestination.getDatasource().getId().equals(id)).findFirst().orElse(null);
        }
        return null;
    }

    public FeedPrecondition getPrecondition() {
        try {
            if (this.node.hasNode(PRECONDITION)) {
                return new JcrFeedPrecondition(this.node.getNode(PRECONDITION), getParentFeed());
            } else {
                return null;
            }
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to retrieve the feed precondition", e);
        }
    }

    public FeedManagerTemplate getTemplate() {
        return getProperty(TEMPLATE, JcrFeedTemplate.class);
    }

    public void setTemplate(FeedManagerTemplate template) {
        setProperty(TEMPLATE, template);
    }

    public List<? extends ServiceLevelAgreement> getServiceLevelAgreements() {
        Set<Node> list = JcrPropertyUtil.getReferencedNodeSet(this.node, SLA);
        List<JcrServiceLevelAgreement> serviceLevelAgreements = new ArrayList<>();
        if (list != null) {
            for (Node n : list) {
                serviceLevelAgreements.add(JcrUtil.createJcrObject(n, JcrServiceLevelAgreement.class));
            }
        }
        return serviceLevelAgreements;
    }

    public void setServiceLevelAgreements(List<? extends ServiceLevelAgreement> serviceLevelAgreements) {
        setProperty(SLA, serviceLevelAgreements);
    }

    public void removeServiceLevelAgreement(ServiceLevelAgreement.ID id) {
        try {
            Set<Node> nodes = JcrPropertyUtil.getSetProperty(this.node, SLA);
            Set<Value> updatedSet = new HashSet<>();
            for (Node node : nodes) {
                if (!node.getIdentifier().equalsIgnoreCase(id.toString())) {
                    Value value = this.node.getSession().getValueFactory().createValue(node, true);
                    updatedSet.add(value);
                }
            }
            node.setProperty(SLA, (Value[]) updatedSet.stream().toArray(size -> new Value[size]));
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Unable to remove reference to SLA " + id + "from feed " + this.getId());
        }

    }

    public boolean addServiceLevelAgreement(ServiceLevelAgreement sla) {
        JcrServiceLevelAgreement jcrServiceLevelAgreement = (JcrServiceLevelAgreement) sla;
        Node node = jcrServiceLevelAgreement.getNode();
        //add a ref to this node
        return JcrPropertyUtil.addToSetProperty(this.node, SLA, node, true);
    }

    public String getJson() {
        return getProperty(FeedDetails.FEED_JSON, String.class);
    }

    public void setJson(String json) {
        setProperty(FeedDetails.FEED_JSON, json);
    }

    public String getNifiProcessGroupId() {
        return getProperty(FeedDetails.PROCESS_GROUP_ID, String.class);
    }

    public void setNifiProcessGroupId(String id) {
        setProperty(FeedDetails.PROCESS_GROUP_ID, id);
    }

    protected JcrFeedSource ensureFeedSource(JcrDatasource datasource) {
        Node feedSrcNode = JcrUtil.addNode(getNode(), FeedDetails.SOURCE_NAME, JcrFeedSource.NODE_TYPE);
        return new JcrFeedSource(feedSrcNode, datasource);
    }
    
    protected JcrFeedDestination ensureFeedDestination(JcrDatasource datasource) {
        Node feedDestNode = JcrUtil.addNode(getNode(), FeedDetails.DESTINATION_NAME, JcrFeedDestination.NODE_TYPE);
        return new JcrFeedDestination(feedDestNode, datasource);
    }
    
    protected void removeFeedSource(JcrFeedSource source) {
        try {
            JcrVersionUtil.checkout(source.getNode().getParent());
            source.getNode().remove();
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("nable to remove feed source for feed " + getParentSummary().getSystemName(), e);
        }
    }
    
    protected void removeFeedDestination(JcrFeedDestination dest) {
        try {
            JcrVersionUtil.checkout(dest.getNode().getParent());
            dest.getNode().remove();
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("nable to remove feed destination for feed " + getParentSummary().getSystemName(), e);
        }
    }

    protected void removeFeedSources() {
        try {
            List<? extends FeedSource> sources = getSources();
            if (sources != null && !sources.isEmpty()) {
                //checkout the feed
                JcrVersionUtil.checkout(getParentSummary().getNode());
                sources.stream().forEach(source -> {
                    try {
                        Node sourceNode = ((JcrFeedSource) source).getNode();
                        ((JcrDatasource) ((JcrFeedSource) source).getDatasource()).removeSourceNode(sourceNode);
                        sourceNode.remove();
                    } catch (RepositoryException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("nable to remove feed sources for feed " + getParentSummary().getSystemName(), e);
        }
    }
    
    protected void removeFeedDestinations() {
        try {
            List<? extends FeedDestination> destinations = getDestinations();

            if (destinations != null && !destinations.isEmpty()) {
                JcrVersionUtil.checkout(getParentSummary().getNode());
                destinations.stream().forEach(dest -> {
                    try {
                        Node destNode = ((JcrFeedDestination) dest).getNode();
                        ((JcrDatasource) ((JcrFeedDestination) dest).getDatasource()).removeDestinationNode(destNode);
                        destNode.remove();

                        ((JcrFeedDestination) dest).getNode().remove();
                    } catch (RepositoryException e) {
                        e.printStackTrace();
                    }
                });


            }
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("unable to remove feed destinations for feed " + getParentSummary().getSystemName(), e);
        }
    }

    protected Node createNewPrecondition() {
        try {
            Node feedNode = getNode();
            Node precondNode = JcrUtil.getOrCreateNode(feedNode, FeedDetails.PRECONDITION, JcrFeed.PRECONDITION_TYPE, true);

            if (precondNode.hasProperty(JcrFeedPrecondition.SLA_REF)) {
                precondNode.getProperty(JcrFeedPrecondition.SLA_REF).remove();
            }
            if (precondNode.hasNode(JcrFeedPrecondition.SLA)) {
                precondNode.getNode(JcrFeedPrecondition.SLA).remove();
            }

            return precondNode.addNode(JcrFeedPrecondition.SLA, JcrFeedPrecondition.SLA_TYPE);
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to create the precondition for feed " + getParentFeed().getId(), e);
        }
    }
}
