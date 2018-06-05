/*
* Copyright [2016-2018] [George Papadakis (gpapadis@yahoo.gr)]
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
 */
package org.scify.jedai.entityclustering;

import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.GomoryHuTree;
import org.scify.jedai.datamodel.SimilarityPairs;

import com.esotericsoftware.minlog.Log;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.jgrapht.alg.ConnectivityInspector;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

/**
 *
 * @author G.A.P. II
 */
public class CutClustering extends AbstractEntityClustering {

    protected double Acap;
    protected SimpleGraph duplicatesGraph;
    protected SimpleWeightedGraph weightedGraph;

    public CutClustering() {
        this(0.3, 0.5);
    }

    public CutClustering(double ac, double simTh) {
        super(simTh);
        Acap = ac;
    }

    @Override
    protected EquivalenceCluster[] getConnectedComponents() {
        // get connected components
        final ConnectivityInspector ci = new ConnectivityInspector(duplicatesGraph);
        final List<Set<Integer>> connectedComponents = ci.connectedSets();

        // prepare output
        int counter = 0;
        final EquivalenceCluster[] equivalenceClusters = new EquivalenceCluster[connectedComponents.size()];
        for (Set<Integer> componentIds : connectedComponents) {
            final EquivalenceCluster newCluster = new EquivalenceCluster();
            if (!isCleanCleanER) {
                newCluster.loadBulkEntityIdsD1(new TIntHashSet(componentIds));
            } else {
                componentIds.forEach((entityId) -> {
                    if (entityId < datasetLimit) {
                        newCluster.addEntityIdD1(entityId);
                    } else {
                        newCluster.addEntityIdD2(entityId - datasetLimit);
                    }
                });
            }
            equivalenceClusters[counter++] = newCluster;
        }

        return equivalenceClusters;
    }

    @Override
    public EquivalenceCluster[] getDuplicates(SimilarityPairs simPairs) {
        initializeData(simPairs);
        similarityGraph = null;
        initializeGraph();

        final Iterator<Comparison> iterator = simPairs.getPairIterator();
        while (iterator.hasNext()) {	// add an edge for every pair of entities with a weight higher than the threshold
            Comparison comparison = iterator.next();
            if (threshold < comparison.getUtilityMeasure()) {
                DefaultWeightedEdge e = (DefaultWeightedEdge) weightedGraph.addEdge(comparison.getEntityId1() + "", (comparison.getEntityId2() + datasetLimit) + "");
                weightedGraph.setEdgeWeight(e, comparison.getUtilityMeasure());
            }
        }

        GomoryHuTree ght = new GomoryHuTree(weightedGraph); //take the minimum cut (Gomory-Hu) tree from the similarity graph
        duplicatesGraph = ght.MinCutTree();
        duplicatesGraph.removeVertex(noOfEntities); //remove the artificial sink

        return getConnectedComponents();
    }

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + threshold + "\t"
                + getParameterName(1) + "=" + Acap;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it partitions the similarity graph into equivalence clusters based on its minimum cut.";
    }

    @Override
    public String getMethodName() {
        return "Cut Clustering";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves two parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".";
    }

    @Override
    public JsonArray getParameterConfiguration() {
        JsonObject obj1 = new JsonObject();
        obj1.put("class", "java.lang.Double");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "0.5");
        obj1.put("minValue", "0.1");
        obj1.put("maxValue", "0.95");
        obj1.put("stepValue", "0.05");
        obj1.put("description", getParameterDescription(0));

        JsonObject obj2 = new JsonObject();
        obj2.put("class", "java.lang.Double");
        obj2.put("name", getParameterName(1));
        obj2.put("defaultValue", "0.3");
        obj2.put("minValue", "0.1");
        obj2.put("maxValue", "0.95");
        obj2.put("stepValue", "0.05");
        obj2.put("description", getParameterDescription(1));

        JsonArray array = new JsonArray();
        array.add(obj1);
        array.add(obj2);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " determines the cut-off similarity threshold for connecting two entities with an edge in the (initial) similarity graph.";
            case 1:
                return "The " + getParameterName(1) + " determines the weight of the capacity edges, which connect every vertex with the artificial sink.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Similarity Threshold";
            case 1:
                return "A-cap";
            default:
                return "invalid parameter id";
        }
    }

    protected void initializeGraph() {
        weightedGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        String sinkLabel = "" + noOfEntities;
        weightedGraph.addVertex(sinkLabel); //add the artificial sink
        for (int i = 0; i < noOfEntities; i++) {
            String edgeLabel = i + "";
            weightedGraph.addVertex(edgeLabel);
            DefaultWeightedEdge e = (DefaultWeightedEdge) weightedGraph.addEdge(sinkLabel, edgeLabel); // add the capacity edges "a"
            weightedGraph.setEdgeWeight(e, Acap); //connecting the artificial sink with all vertices
        }

        Log.info("Added " + noOfEntities + " nodes in the graph");
    }

    public void setA(double Acap) {
        this.Acap = Acap;
    }
}
