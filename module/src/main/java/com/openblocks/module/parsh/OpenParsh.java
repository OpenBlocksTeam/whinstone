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
import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * OpenParsh is meant to be an efficient parser where it converts both code and layout into
 * a byte-level format, where it will save some storage space
 */
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

    /* To generate a unique ID, we're just going to use a random generator that generates a random
     * identifier fixed at 16 characters, which can hold about 65536 projects */
    @Override
    public String generateFreeId(ArrayList<String> existing_ids) {
        String result;

        do {
            final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            SecureRandom rnd = new SecureRandom();

            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 16; i++)
                sb.append(AB.charAt(rnd.nextInt(AB.length())));

            result = sb.toString();

        } while (!existing_ids.contains(result));

        return result;
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
