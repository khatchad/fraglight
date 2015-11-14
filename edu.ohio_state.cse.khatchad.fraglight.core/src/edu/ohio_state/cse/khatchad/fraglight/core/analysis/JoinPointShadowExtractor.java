/**
 * 
 */
package edu.ohio_state.cse.khatchad.fraglight.core.analysis;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aspectj.lang.JoinPoint;
import org.eclipse.ajdt.core.javaelements.AJCodeElement;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.internal.core.JavaElement;

import ca.mcgill.cs.swevo.jayfx.ConversionException;
import edu.ohio_state.cse.khatchad.fraglight.core.util.Util;

/**
 * @author <a href="mailto:khatchad@cse.ohio-state.edu">Raffi Khatchadourian</a>
 * 
 */
public class JoinPointShadowExtractor extends ASTVisitor {

	private Set<IJavaElement> joinPointShadows = new LinkedHashSet<IJavaElement>();

	@Override
	public boolean visit(MethodInvocation node) {
		try {
			this.joinPointShadows.add(getJavaElement(node));
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (ConversionException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(SuperMethodInvocation node) {
		try {
			this.joinPointShadows.add(getJavaElement(node));
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (ConversionException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(ClassInstanceCreation node) {
		try {
			this.joinPointShadows.add(getJavaElement(node));
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (ConversionException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(ConstructorInvocation node) {
		try {
			this.joinPointShadows.add(getJavaElement(node));
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (ConversionException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(SuperConstructorInvocation node) {
		try {
			this.joinPointShadows.add(getJavaElement(node));
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (ConversionException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(final QualifiedName node) {
		IBinding binding = node.resolveBinding();

		if (binding == null) {
			logger.log(Level.WARNING, "Unable to extract field access join point from " + node, node);
			return false;
		}

		if (binding.getKind() == IBinding.VARIABLE) {
			IVariableBinding variableBinding = (IVariableBinding) binding;
			MethodDeclaration methodDeclaration = Util.getMethodDeclaration(node);

			if (variableBinding.isField() && methodDeclaration != null) {
				final Assignment assignment = getAssignment(node);
				if (assignment != null) {
					try {
						this.joinPointShadows.add(getFieldSetJavaElement(node));
					} catch (JavaModelException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					} catch (ConversionException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}

					if (!(assignment.getOperator() == Assignment.Operator.ASSIGN))
						try {
							this.joinPointShadows.add(getFieldGetJavaElement(node));
						} catch (JavaModelException e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						} catch (ConversionException e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
				} else
					try {
						this.joinPointShadows.add(getFieldGetJavaElement(node));
					} catch (JavaModelException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					} catch (ConversionException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
			}
		}
		return false;
	}

	@Override
	public boolean visit(final SimpleName node) {
		IBinding binding = node.resolveBinding();

		if (binding == null) {
			logger.log(Level.WARNING, "Unable to extract field access join point from " + node, node);
			return false;
		}

		if (binding.getKind() == IBinding.VARIABLE) {
			IVariableBinding variableBinding = (IVariableBinding) binding;
			MethodDeclaration methodDeclaration = Util.getMethodDeclaration(node);

			if (variableBinding.isField() && methodDeclaration != null) {
				final Assignment assignment = getAssignment(node);
				if (assignment != null) {
					try {
						this.joinPointShadows.add(getFieldSetJavaElement(node));
					} catch (JavaModelException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					} catch (ConversionException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}

					if (!(assignment.getOperator() == Assignment.Operator.ASSIGN))
						try {
							this.joinPointShadows.add(getFieldGetJavaElement(node));
						} catch (JavaModelException e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						} catch (ConversionException e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
				} else
					try {
						this.joinPointShadows.add(getFieldGetJavaElement(node));
					} catch (JavaModelException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					} catch (ConversionException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
			}
		}
		return false;
	}

	private static Logger logger = Logger.getLogger(JoinPointShadowExtractor.class.getName());

	@Override
	public boolean visit(FieldAccess node) {
		Assignment assignment = getAssignment(node);
		if (assignment != null) {
			// it's a field set.
			try {
				this.joinPointShadows.add(getFieldSetJavaElement(node));
			} catch (JavaModelException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			} catch (ConversionException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

			if (!(assignment.getOperator() == Assignment.Operator.ASSIGN))
				try {
					this.joinPointShadows.add(getFieldGetJavaElement(node));
				} catch (JavaModelException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				} catch (ConversionException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
		} else
			try {
				this.joinPointShadows.add(getFieldGetJavaElement(node));
			} catch (JavaModelException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			} catch (ConversionException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

		return super.visit(node);
	}

	@Override
	public boolean visit(SuperFieldAccess node) {
		Assignment assignment = getAssignment(node);
		if (assignment != null) {
			// it's a field set.
			try {
				this.joinPointShadows.add(getFieldSetJavaElement(node));
			} catch (JavaModelException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			} catch (ConversionException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

			if (!(assignment.getOperator() == Assignment.Operator.ASSIGN))
				try {
					this.joinPointShadows.add(getFieldGetJavaElement(node));
				} catch (JavaModelException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				} catch (ConversionException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
		} else
			try {
				this.joinPointShadows.add(getFieldGetJavaElement(node));
			} catch (JavaModelException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			} catch (ConversionException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

		return super.visit(node);
	}

	private static IJavaElement getFieldSetJavaElement(SimpleName node) throws JavaModelException, ConversionException {
		IBinding binding = node.resolveBinding();
		AJCodeElement ret = getJavaElement(node, binding, JoinPoint.FIELD_SET);
		return ret;
	}

	private static IJavaElement getFieldGetJavaElement(SimpleName node) throws JavaModelException, ConversionException {
		IBinding binding = node.resolveBinding();
		AJCodeElement ret = getJavaElement(node, binding, JoinPoint.FIELD_GET);
		return ret;
	}

	private static IJavaElement getFieldSetJavaElement(QualifiedName node)
			throws JavaModelException, ConversionException {
		IBinding binding = node.resolveBinding();
		AJCodeElement ret = getJavaElement(node, binding, JoinPoint.FIELD_SET);
		return ret;
	}

	private static IJavaElement getFieldGetJavaElement(QualifiedName node)
			throws JavaModelException, ConversionException {
		IBinding binding = node.resolveBinding();
		AJCodeElement ret = getJavaElement(node, binding, JoinPoint.FIELD_GET);
		return ret;
	}

	private static IJavaElement getFieldSetJavaElement(FieldAccess node)
			throws JavaModelException, ConversionException {
		IBinding fieldBinding = node.resolveFieldBinding();
		AJCodeElement ret = getJavaElement(node, fieldBinding, JoinPoint.FIELD_SET);
		return ret;
	}

	private static IJavaElement getFieldSetJavaElement(SuperFieldAccess node)
			throws JavaModelException, ConversionException {
		IBinding fieldBinding = node.resolveFieldBinding();
		AJCodeElement ret = getJavaElement(node, fieldBinding, JoinPoint.FIELD_SET);
		return ret;
	}

	private static IJavaElement getFieldGetJavaElement(FieldAccess node)
			throws JavaModelException, ConversionException {
		IBinding fieldBinding = node.resolveFieldBinding();
		AJCodeElement ret = getJavaElement(node, fieldBinding, JoinPoint.FIELD_GET);
		return ret;
	}

	private static IJavaElement getFieldGetJavaElement(SuperFieldAccess node)
			throws JavaModelException, ConversionException {
		IBinding fieldBinding = node.resolveFieldBinding();
		AJCodeElement ret = getJavaElement(node, fieldBinding, JoinPoint.FIELD_GET);
		return ret;
	}

	private static IJavaElement getJavaElement(ConstructorInvocation node)
			throws JavaModelException, ConversionException {
		IBinding methodBinding = node.resolveConstructorBinding();
		AJCodeElement ret = getJavaElement(node, methodBinding, JoinPoint.CONSTRUCTOR_CALL);
		return ret;
	}

	private static IJavaElement getJavaElement(SuperConstructorInvocation node)
			throws JavaModelException, ConversionException {
		IBinding methodBinding = node.resolveConstructorBinding();
		AJCodeElement ret = getJavaElement(node, methodBinding, JoinPoint.CONSTRUCTOR_CALL);
		return ret;
	}

	private static IJavaElement getJavaElement(ClassInstanceCreation node)
			throws JavaModelException, ConversionException {
		IBinding methodBinding = node.resolveConstructorBinding();
		AJCodeElement ret = getJavaElement(node, methodBinding, JoinPoint.CONSTRUCTOR_CALL);
		return ret;
	}

	private static IJavaElement getJavaElement(SuperMethodInvocation node)
			throws JavaModelException, ConversionException {
		IBinding methodBinding = node.resolveMethodBinding();
		AJCodeElement ret = getJavaElement(node, methodBinding, JoinPoint.METHOD_CALL);
		return ret;
	}

	private static IJavaElement getJavaElement(MethodInvocation node) throws JavaModelException, ConversionException {
		IBinding methodBinding = node.resolveMethodBinding();
		AJCodeElement ret = getJavaElement(node, methodBinding, JoinPoint.METHOD_CALL);
		return ret;
	}

	private static AJCodeElement getJavaElement(ASTNode node, IBinding binding, String joinPointType)
			throws JavaModelException, ConversionException {
		String ajTargetString = getAJTargetString(binding, joinPointType);
		IJavaElement source = getSourceMethod(node);
		AJCodeElement ret = new AJCodeElement((JavaElement) source, ajTargetString);
		return ret;
	}

	private static String getAJTargetString(IBinding methodBinding, String joinPointType)
			throws JavaModelException, ConversionException {
		IJavaElement target = methodBinding.getJavaElement();
		StringBuilder targetStringBuilder = new StringBuilder(Util.getTargetString(target));
		targetStringBuilder.insert(0, joinPointType + "(");
		targetStringBuilder.append(')');
		String ajTargetString = targetStringBuilder.toString();
		return ajTargetString;
	}

	private static IJavaElement getSourceMethod(ASTNode node) {
		MethodDeclaration methodDeclaration = Util.getMethodDeclaration(node);
		IJavaElement source = methodDeclaration.resolveBinding().getMethodDeclaration().getJavaElement();
		return source;
	}

	public Set<IJavaElement> getJoinPointShadows() {
		return joinPointShadows;
	}

	private static Assignment getAssignment(final ASTNode node) {
		if (node == null)
			return null;

		if (node.getNodeType() == ASTNode.ASSIGNMENT)
			return (Assignment) node;

		else
			return getAssignment(node.getParent());
	}
}