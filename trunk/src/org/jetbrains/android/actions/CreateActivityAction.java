package org.jetbrains.android.actions;

import com.intellij.ide.actions.CreateElementActionBase;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.android.dom.manifest.*;
import org.jetbrains.android.dom.manifest.Action;
import org.jetbrains.android.dom.resources.ResourceValue;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author yole
 */
public class CreateActivityAction extends CreateElementActionBase {
    private JTextField myActivityNameTextField;
    private JPanel myPanel;
    private JCheckBox myMarkAsStartupActivityCheckBox;
    private JTextField myLabelTextField;

    public CreateActivityAction() {
        super("Activity", "Create new Android activity", AndroidManager.ANDROID_ICON);
    }

    public void update(AnActionEvent e) {
        Module module = e.getData(DataKeys.MODULE);
        e.getPresentation().setVisible(AndroidFacet.getInstance(module) != null);
    }

    @NotNull
    protected PsiElement[] invokeDialog(Project project, PsiDirectory directory) {
        myActivityNameTextField.setText("");
        myLabelTextField.setText("");
        myMarkAsStartupActivityCheckBox.setSelected(isFirstActivity(directory));

        final MyInputValidator validator = new MyInputValidator(project, directory);
        DialogWrapper dialog = new MyDialog(project, validator);
        dialog.show();
        return validator.getCreatedElements();
    }

    protected void checkBeforeCreate(String newName, PsiDirectory directory) throws IncorrectOperationException {
        directory.checkCreateClass(newName);
    }

    @NotNull
    protected PsiElement[] create(String newName, PsiDirectory directory) throws Exception {
        PsiClass aClass = directory.createClass(newName);
        PsiClass activityClass = directory.getManager().findClass("android.app.Activity", directory.getProject().getAllScope());
        if (activityClass != null) {
            PsiJavaCodeReferenceElement activityReference = directory.getManager().getElementFactory().createClassReferenceElement(activityClass);
            aClass.getExtendsList().add(activityReference);
        }
        Module module = VfsUtil.getModuleForFile(directory.getProject(), directory.getVirtualFile());
        if (module != null) {
            AndroidFacet facet = AndroidFacet.getInstance(module);
            assert facet != null;
            registerActivity(aClass, directory.getPackage(), facet);
        }
        return new PsiElement[] { aClass };
    }

    private static boolean isFirstActivity(PsiDirectory directory) {
        Module module = VfsUtil.getModuleForFile(directory.getProject(), directory.getVirtualFile());
        if (module == null) return false;
        AndroidFacet facet = AndroidFacet.getInstance(module);
        Manifest manifest = facet.getManifest();
        if (manifest == null) return false;
        Application application = manifest.getApplication();
        return application != null && application.getActivities().isEmpty();
    }

    private void registerActivity(PsiClass aClass, PsiPackage aPackage, AndroidFacet facet) {
        Manifest manifest = facet.getManifest();
        if (manifest == null) return;
        String packageName = manifest.getPackage().getValue();
        if (packageName == null || packageName.length() == 0) {
            manifest.getPackage().setValue(aPackage.getQualifiedName());
        }
        Application application = manifest.getApplication();
        if (application == null) return;
        Activity activity = application.addActivity();
        activity.getActivityClass().setValue(aClass);
        if (myLabelTextField.getText().length() > 0) {
            activity.getLabel().setValue(ResourceValue.literal(myLabelTextField.getText()));
        }

        if (myMarkAsStartupActivityCheckBox.isSelected()) {
            IntentFilter filter = activity.addIntentFilter();
            Action action = filter.addAction();
            action.getValue().setValue("android.intent.action.MAIN");
            Category category = filter.addCategory();
            category.getValue().setValue("android.intent.category.LAUNCHER");
        }
    }

    protected String getErrorTitle() {
        return "Cannot create activity";
    }

    protected String getCommandName() {
        return "Create Activity";
    }

    protected String getActionName(PsiDirectory directory, String newName) {
        return "Creating activity " + newName;
    }

    private class MyDialog extends DialogWrapper {
        private final MyInputValidator myValidator;

        public MyDialog(Project project, MyInputValidator validator) {
            super(project, true);
            myValidator = validator;
            init();
            setTitle("Create Activity");
        }

        @Nullable
        protected JComponent createCenterPanel() {
            return myPanel;
        }

        protected void doOKAction() {
            final String inputString = myActivityNameTextField.getText().trim();
            if (myValidator.checkInput(inputString) && myValidator.canClose(inputString)) {
                close(OK_EXIT_CODE);
            }
            close(OK_EXIT_CODE);
        }

        public JComponent getPreferredFocusedComponent() {
            return myActivityNameTextField;
        }
    }
}
