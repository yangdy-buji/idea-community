/*
 * Copyright 2003-2008 Dave Griffith, Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ig.performance;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ui.SingleCheckboxOptionsPanel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Query;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.InspectionGadgetsFix;
import com.siyeh.ig.psiutils.ClassUtils;
import com.siyeh.ig.psiutils.MethodUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CallToSimpleGetterInClassInspection extends BaseInspection {

    /** @noinspection PublicField*/
    public boolean ignoreGetterCallsOnOtherObjects = false;

    @Override
    @NotNull
    public String getID(){
        return "CallToSimpleGetterFromWithinClass";
    }

    @Override
    @NotNull
    public String getDisplayName(){
        return InspectionGadgetsBundle.message(
                "call.to.simple.getter.in.class.display.name");
    }

    @Override
    @NotNull
    public String buildErrorString(Object... infos){
        return InspectionGadgetsBundle.message(
                "call.to.simple.getter.in.class.problem.descriptor");
    }

    @Override
    @Nullable
    public JComponent createOptionsPanel() {
        return new SingleCheckboxOptionsPanel(InspectionGadgetsBundle.message(
                "call.to.simple.getter.in.class.display.name.ignore.option"),
                this, "ignoreGetterCallsOnOtherObjects");
    }

    @Override
    public InspectionGadgetsFix buildFix(Object... infos){
        return new InlineCallFix();
    }

    private static class InlineCallFix extends InspectionGadgetsFix{

        @NotNull
        public String getName(){
            return InspectionGadgetsBundle.message(
                    "call.to.simple.getter.in.class.inline.quickfix");
        }

        @Override
        public void doFix(Project project, ProblemDescriptor descriptor)
                throws IncorrectOperationException{
            final PsiElement methodIdentifier = descriptor.getPsiElement();
            final PsiReferenceExpression methodExpression =
                    (PsiReferenceExpression) methodIdentifier.getParent();
            if (methodExpression == null){
                return;
            }
            final PsiMethodCallExpression call =
                    (PsiMethodCallExpression) methodExpression.getParent();
            if (call == null){
                return;
            }
            final PsiMethod method = call.resolveMethod();
            if (method == null){
                return;
            }
            final PsiCodeBlock body = method.getBody();
            if (body == null){
                return;
            }
            final PsiStatement[] statements = body.getStatements();
            final PsiReturnStatement returnStatement =
                    (PsiReturnStatement) statements[0];
            final PsiExpression returnValue =
                    returnStatement.getReturnValue();
            if (!(returnValue instanceof PsiReferenceExpression)){
                return;
            }
            final PsiReferenceExpression referenceExpression =
                    (PsiReferenceExpression) returnValue;
            final PsiField field = (PsiField) referenceExpression.resolve();
            if (field == null){
                return;
            }
            final String fieldName = field.getName();
            if (fieldName == null) {
                return;
            }
            final PsiExpression qualifier =
                    methodExpression.getQualifierExpression();
            if(qualifier == null){
                final JavaPsiFacade facade = JavaPsiFacade.getInstance(call.getProject());
                final PsiResolveHelper resolveHelper = facade.getResolveHelper();
                final PsiVariable variable =
                        resolveHelper.resolveReferencedVariable(fieldName,
                                call);
                if (variable == null){
                    return;
                }
                if (variable.equals(field)){
                    replaceExpression(call, fieldName);
                } else{
                    replaceExpression(call, "this." + fieldName);
                }
            } else{
                replaceExpression(call, qualifier.getText() + '.' + fieldName);
            }
        }
    }

    @Override
    public BaseInspectionVisitor buildVisitor(){
        return new CallToSimpleGetterInClassVisitor();
    }

    private class CallToSimpleGetterInClassVisitor
            extends BaseInspectionVisitor{

        @Override public void visitMethodCallExpression(
                @NotNull PsiMethodCallExpression call){
            super.visitMethodCallExpression(call);
            final PsiClass containingClass =
                    ClassUtils.getContainingClass(call);
            if(containingClass == null){
                return;
            }
            final PsiMethod method = call.resolveMethod();
            if(method == null){
                return;
            }
            if(!containingClass.equals(method.getContainingClass())){
                return;
            }
            final PsiReferenceExpression methodExpression =
                    call.getMethodExpression();
            final PsiExpression qualifier =
                    methodExpression.getQualifierExpression();
            if (qualifier != null &&
                    !(qualifier instanceof PsiThisExpression)) {
                if (ignoreGetterCallsOnOtherObjects) {
                    return;
                }
                final PsiType type = qualifier.getType();
                if (!(type instanceof PsiClassType)) {
                    return;
                }
                final PsiClassType classType = (PsiClassType)type;
                final PsiClass qualifierClass = classType.resolve();
                if (!containingClass.equals(qualifierClass)) {
                    return;
                }
            }
            if(!MethodUtils.isSimpleGetter(method)){
                return;
            }
            final Query<PsiMethod> query = OverridingMethodsSearch.search(
                    method, method.getUseScope(), true);
            final PsiMethod overridingMethod = query.findFirst();
            if (overridingMethod != null) {
                return;
            }
            registerMethodCallError(call);
        }
    }
}