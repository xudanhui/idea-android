package org.jetbrains.android.dom;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.builders.ModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import junit.framework.TestCase;
import org.jetbrains.android.facet.AndroidFacet;

import java.io.File;

/**
 * @author coyote
 */
abstract class AndroidCompletionTest extends TestCase {
    protected CodeInsightTestFixture myFixture;
    protected AndroidFacet facet;
    private final String activeFolder;

    public AndroidCompletionTest(String activeFolder) {
        this.activeFolder = activeFolder;
    }

    private static String getCompletionTestDataPath() {
        return getHomePath() + "/testData/completion";
    }

    private static String getHomePath() {
        return PathManager.getHomePath().replace(File.separatorChar, '/');
    }

    private static String getTestSkdPath() {
        return getHomePath() + "/testData/sdk";
    }

    public void setUp() throws Exception {
        final TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder();
        ModuleFixtureBuilder moduleBuilder = projectBuilder.addModule(JavaModuleFixtureBuilder.class);
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(projectBuilder.getFixture());
        myFixture.setTestDataPath(getCompletionTestDataPath() + '/' + activeFolder + '/');
        myFixture.setUp();
        moduleBuilder.addContentRoot(myFixture.getTempDirPath());
        addFacet();
    }

    private void addFacet() {
        FacetManager facetManager = FacetManager.getInstance(myFixture.getModule());
        facet = facetManager.createFacet(AndroidFacet.getFacetType(), "Android", null);
        facet.getConfiguration().setSdkPath(getTestSkdPath());
        final ModifiableFacetModel model = facetManager.createModifiableModel();
        model.addFacet(facet);
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                model.commit();
            }
        });
    }
}

