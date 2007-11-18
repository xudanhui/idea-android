package org.jetbrains.android.compiler;

import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.AndroidManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yole
 */
public class ResourcesValidityState implements ValidityState {
    private Map<String, Long> myResourceTimestamps = new HashMap<String, Long>();
    private long myClassesTimestamp;

    public ResourcesValidityState(Module module, boolean includeClassesDex) {
        myClassesTimestamp = -1;
        if (includeClassesDex) {
            VirtualFile outputPath = ModuleRootManager.getInstance(module).getCompilerOutputPath();
            if (outputPath != null) {
                VirtualFile classesDex = outputPath.findChild(AndroidManager.CLASSES_FILE_NAME);
                if (classesDex != null) {
                    myClassesTimestamp = classesDex.getTimeStamp();
                }
            }
        }
        AndroidFacet facet = AndroidFacet.getInstance(module);
        if (facet == null) return;
        VirtualFile resourcesDir = facet.getResourcesDir();
        if (resourcesDir == null) return;
        collectResourceFiles(resourcesDir, "");
    }

    private void collectResourceFiles(VirtualFile resourcesDir, String relativePath) {
        for(VirtualFile child: resourcesDir.getChildren()) {
            String path = relativePath + "/" + child.getName();
            if (child.isDirectory()) {
                collectResourceFiles(child, path);
            }
            else {
                myResourceTimestamps.put(path, child.getTimeStamp());
            }
        }
    }

    public ResourcesValidityState(DataInputStream is) throws IOException {
        myClassesTimestamp = is.readLong();
        int count = is.readInt();
        for(int i=0; i<count; i++) {
            String path = is.readUTF();
            long timestamp = is.readLong();
            myResourceTimestamps.put(path, timestamp);
        }
    }

    public boolean equalsTo(ValidityState otherState) {
        if (!(otherState instanceof ResourcesValidityState)) {
            return false;
        }
        ResourcesValidityState rhs = (ResourcesValidityState) otherState;
        return myResourceTimestamps.equals(rhs.myResourceTimestamps) && myClassesTimestamp == rhs.myClassesTimestamp;
    }

    public void save(DataOutputStream os) throws IOException {
        os.writeLong(myClassesTimestamp);
        os.writeInt(myResourceTimestamps.size());
        for(Map.Entry<String, Long> e: myResourceTimestamps.entrySet()) {
            os.writeUTF(e.getKey());
            os.writeLong(e.getValue());
        }
    }
}
