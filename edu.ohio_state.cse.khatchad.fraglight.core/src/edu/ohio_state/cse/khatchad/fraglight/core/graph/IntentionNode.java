/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.graph;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.jdom.DataConversionException;
import org.jdom.Element;

import ca.mcgill.cs.swevo.jayfx.ConversionException;
import ca.mcgill.cs.swevo.jayfx.JayFX;
import ca.mcgill.cs.swevo.jayfx.model.FlyweightElementFactory;
import ca.mcgill.cs.swevo.jayfx.model.IElement;
import ca.mcgill.cs.swevo.jayfx.model.MethodElement;
import ca.mcgill.cs.swevo.jayfx.model.Relation;

/**
 * @author raffi
 * 
 */
public class IntentionNode<E extends IElement> extends GraphElement<E> {

	private static final long serialVersionUID = -5215134933494372784L;

	public static final IntentionNode<IElement> DISABLED_WILDCARD = new IntentionNode<IElement>(new WildcardElement());

	public static final IntentionNode<IElement> ENABLED_WILDCARD = new IntentionNode<IElement>(new WildcardElement(),
			true);

	private E elem;

	private final Set<IntentionArc<E>> arcs = new HashSet<IntentionArc<E>>();

	private final Map<Relation, Set<IntentionArc<E>>> relationToArcSetMap = new LinkedHashMap<Relation, Set<IntentionArc<E>>>();

	private IntentionNode() {
		initializeRelationToArcSetMap();
	}

	private void initializeRelationToArcSetMap() {
		for (Relation relation : Relation.values())
			this.relationToArcSetMap.put(relation, new LinkedHashSet<IntentionArc<E>>());
	}

	/**
	 * @param elem
	 */
	public IntentionNode(final E elem) {
		this();
		this.elem = elem;
	}

	public IntentionNode(E elem, boolean enabled) {
		this(elem);
		if (enabled)
			this.enable();
		else
			this.disable();
	}

	/**
	 * @param xmlElem
	 * @throws DataConversionException
	 */
	@SuppressWarnings("unchecked")
	public IntentionNode(Element xmlElem) throws DataConversionException {
		super(xmlElem);
		Element elementXML = xmlElem.getChild(IElement.class.getSimpleName());
		this.elem = FlyweightElementFactory.getElement(elementXML);
		initializeRelationToArcSetMap();
	}

	/**
	 * @param intentionNode
	 */
	public void addArc(final IntentionArc<E> intentionArc) {
		this.arcs.add(intentionArc);

		if (!this.relationToArcSetMap.containsKey(intentionArc.getType()))
			this.relationToArcSetMap.put(intentionArc.getType(), new LinkedHashSet<IntentionArc<E>>());

		this.relationToArcSetMap.get(intentionArc.getType()).add(intentionArc);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof IntentionNode ? this.elem.equals(((IntentionNode) obj).elem) : false;
	}

	/**
	 * @return the arcs
	 */
	public Set<IntentionArc<E>> getArcs() {
		return this.arcs;
	}

	/**
	 * @return the elem
	 */
	public E getElem() {
		return this.elem;
	}

	/**
	 * @param advises
	 * @return
	 */
	// public boolean hasEdge(final Relation relation) {
	// for (final IntentionArc<E> arc : this.arcs)
	// if (arc.getType().equals(relation))
	// return true;
	// return false;
	// }

	/**
	 * @param relation
	 * @return
	 */
	// public boolean hasEnabledEdgesForIncommingRelation(final Relation
	// relation) {
	// return this.elem.hasEnabledRelationFor(relation);
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.elem.hashCode();
	}

	public String toDotFormat() {
		final StringBuilder ret = new StringBuilder();
		ret.append(this.hashCode());
		ret.append(" [label=\"");
		ret.append(this.elem.getShortName());
		ret.append('"');
		if (this.isEnabled())
			ret.append(",style=filled,color=red,fontcolor=white");
		ret.append("];");
		ret.append('\n');

		int edgeCount = 0;
		for (final IntentionArc<E> edge : this.arcs) {
			ret.append(edge.toDotFormat());
			if (edgeCount++ < this.arcs.size() - 1)
				ret.append('\n');
		}
		return ret.toString();
	}

	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		// ret.append('(');
		ret.append(super.toString());
		ret.append(this.elem.getShortName());
		// ret.append(')');
		return ret.toString();
	}

	/**
	 * @return
	 */
	@Override
	public Element getXML() {
		Element ret = super.getXML();
		ret.addContent(this.elem.getXML());
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.lancs.comp.khatchad.rejuvenatepc.core.graph.IntentionElement#
	 * getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		StringBuilder ret = new StringBuilder();
		ret.append(super.toString());
		ret.append(this.elem.getId());
		return ret.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.lancs.comp.khatchad.rejuvenatepc.core.graph.IntentionElement#
	 * getPrettyString()
	 */
	@Override
	public String toPrettyString() {
		return this.toString();
	}

	/**
	 * @param targetNode
	 * @param calls
	 * @return
	 */
	public IntentionArc<E> getArc(IntentionNode<IElement> targetNode, Relation relation) {
		for (IntentionArc<E> arc : this.relationToArcSetMap.get(relation))
			if (arc.getToNode().equals(targetNode))
				return arc;
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.lancs.comp.khatchad.rejuvenatepc.core.graph.IntentionElement#
	 * toJavaElement()
	 */
	@Override
	public IJavaElement toJavaElement(JayFX database) {
		IJavaElement javaElement = null;
		try {
			javaElement = database.convertToJavaElement(this.elem);
		} catch (ConversionException e) {
		}
		return javaElement;
	}

	/**
	 * @return
	 */
	@Override
	public boolean isAdvisable() {
		return this.getElem() instanceof MethodElement;
	}
}