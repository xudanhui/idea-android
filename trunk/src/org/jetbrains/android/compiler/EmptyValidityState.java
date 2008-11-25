package org.jetbrains.android.compiler;

import com.intellij.openapi.compiler.ValidityState;

import java.io.DataOutput;
import java.io.IOException;

/**
 * @author yole
 */
public class EmptyValidityState implements ValidityState {
    public boolean equalsTo(ValidityState validityState) {
        return validityState == this;
    }

    public void save(DataOutput dataOutput) throws IOException {
    }
}
