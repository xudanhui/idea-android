package org.jetbrains.android.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.android.AndroidManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author yole
 */
public class AndroidRunConfigurationType implements ConfigurationType {
    private static class AndroidRunConfigurationFactory extends ConfigurationFactory {
        protected AndroidRunConfigurationFactory(ConfigurationType type) {
            super(type);
        }

        public RunConfiguration createTemplateConfiguration(Project project) {
            return new AndroidRunConfiguration("", project, this);
        }
    }

    private AndroidRunConfigurationFactory myFactory = new AndroidRunConfigurationFactory(this);

    public static AndroidRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(AndroidRunConfigurationType.class);
    }

    public String getDisplayName() {
        return "Android";
    }

    public String getConfigurationTypeDescription() {
        return "Android run configuration";
    }

    public Icon getIcon() {
        return AndroidManager.ANDROID_ICON;
    }

    @NotNull
    public String getId() {
        return "AndroidRunConfigurationType";
    }

    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[] { myFactory };
    }

    public AndroidRunConfigurationFactory getFactory() {
        return myFactory;
    }
}
