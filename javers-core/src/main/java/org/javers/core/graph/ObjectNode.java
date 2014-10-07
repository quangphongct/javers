package org.javers.core.graph;

import org.javers.common.collections.Optional;
import org.javers.common.patterns.visitors.Visitable;
import org.javers.common.validation.Validate;
import org.javers.core.metamodel.object.Cdo;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.CdoWrapper;
import org.javers.core.metamodel.object.GlobalId;
import org.javers.core.metamodel.property.Entity;
import org.javers.core.metamodel.property.ManagedClass;
import org.javers.core.metamodel.property.Property;
import org.javers.core.metamodel.property.ValueObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.javers.common.validation.Validate.argumentIsNotNull;
import static org.javers.core.metamodel.object.InstanceId.createFromInstance;

/**
 * Node in client's domain objects graph. Reflects one {@link Cdo} or {@link CdoSnapshot}.
 * <p/>
 * Cdo could be an {@link Entity} or a {@link ValueObject}
 * <p/>
 * Implementation should delegate equals() and hashCode() to {@link Cdo}
 *
 * @author bartosz walacik
 */
public class ObjectNode implements Visitable<GraphVisitor> {
    private final Cdo cdo;
    private final Map<Property, Edge> edges = new HashMap<>();
    private boolean stub;

    public ObjectNode(Cdo cdo) {
        argumentIsNotNull(cdo);
        this.cdo = cdo;
        this.stub = true;
    }

    ObjectNode(Object cdo, Entity entity) {
        this(new CdoWrapper(cdo, createFromInstance(cdo, entity)));
    }

    /**
     * @return never returns null
     */
    //Cdo getCdo();

    /**
     * @return returns {@link Optional#EMPTY} for snapshots
     */
    public Optional<Object> wrappedCdo() {
        return cdo.getWrappedCdo();
    }

    /**
     * shortcut to {@link Cdo#getGlobalId()}
     */
    public GlobalId getGlobalId() {
        return cdo.getGlobalId();
    }

    /**
     * only for properties with return type: ManagedType
     */
    public GlobalId getReference(Property property){
        Edge edge = getEdge(property); //could be null for snapshots

        //TODO this is ugly, how to move this logic to Cdo implementations?
        if (edge != null && edge instanceof SingleEdge){
            return ((SingleEdge)edge).getReference().getGlobalId();
        }
        else {
            return (GlobalId)getPropertyValue(property);
        }
    }

    public Object getPropertyValue(Property property) {
        Validate.argumentIsNotNull(property);
        return cdo.getPropertyValue(property);
    }

    public boolean isNull(Property property){
        Validate.argumentIsNotNull(property);
        return cdo.isNull(property);
    }

    Edge getEdge(Property property) {
        return edges.get(property);
    }

    int edgesCount() {
        return edges.size();
    }

    /**
     * shortcut to {@link Cdo#getManagedClass()}
     */
    public ManagedClass getManagedClass() {
        return cdo.getManagedClass();
    }


    Cdo getCdo() {
        return cdo;
    }

    boolean isStub() {
        return stub;
    }

    void unstub() {
        stub = false;
    }

    void addEdge(Edge edge) {
        this.edges.put(edge.getProperty(), edge);
    }

    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ObjectNode that = (ObjectNode) o;
        return cdo.equals(that.cdo);
    }

    public int hashCode() {
        return cdo.hashCode();
    }

    @Override
    public void accept(GraphVisitor visitor) {
        if(visitor.wasVisited(this)){
            return;
        }

        visitor.visit(this);

        for(Edge edge : edges.values()) {
            edge.accept(visitor);
        }
    }
}