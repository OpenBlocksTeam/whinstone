package com.openblocks.module.parsh;

import android.content.Context;
import android.util.Pair;

import com.openblocks.moduleinterface.OpenBlocksModule;
import com.openblocks.moduleinterface.models.OpenBlocksProjectMetadata;
import com.openblocks.moduleinterface.models.OpenBlocksRawProject;
import com.openblocks.moduleinterface.models.config.OpenBlocksConfig;
import com.openblocks.moduleinterface.projectfiles.OpenBlocksCode;
import com.openblocks.moduleinterface.projectfiles.OpenBlocksLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class OpenParsh implements OpenBlocksModule.ProjectParser {

    WeakReference<Context> context;

    @Override
    public Type getType() {
        return Type.PROJECT_PARSER;
    }

    @Override
    public void initialize(Context context) {
        this.context = new WeakReference<>(context);
    }

    @Override
    public OpenBlocksConfig setupConfig() {
        return new OpenBlocksConfig();
    }

    @Override
    public void applyConfig(OpenBlocksConfig config) {

    }

    @Override
    public String generateFreeId(ArrayList<String> existing_ids) {
        return null;
    }

    @Override
    public Pair<OpenBlocksCode, OpenBlocksLayout> initializeEmptyProject() {
        return null;
    }

    @Override
    public OpenBlocksLayout parseLayout(OpenBlocksRawProject project) {
        return null;
    }

    @Override
    public OpenBlocksCode parseCode(OpenBlocksRawProject project) {
        return null;
    }

    @Override
    public OpenBlocksProjectMetadata parseMetadata(OpenBlocksRawProject project) {
        return null;
    }

    @Override
    public OpenBlocksRawProject saveProject(OpenBlocksProjectMetadata metadata, OpenBlocksCode code, OpenBlocksLayout layout) {
        return null;
    }
}
