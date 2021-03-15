package com.openblocks.module.parsh;

import android.content.Context;
import android.graphics.Path;
import android.util.Pair;

import com.openblocks.moduleinterface.OpenBlocksModule;
import com.openblocks.moduleinterface.models.OpenBlocksFile;
import com.openblocks.moduleinterface.models.OpenBlocksProjectMetadata;
import com.openblocks.moduleinterface.models.OpenBlocksRawProject;
import com.openblocks.moduleinterface.models.config.OpenBlocksConfig;
import com.openblocks.moduleinterface.projectfiles.OpenBlocksCode;
import com.openblocks.moduleinterface.projectfiles.OpenBlocksLayout;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * OpenParsh is meant to be an efficient parser where it converts both code and layout into
 * a byte-level format, where it will save some storage space
 */
public class OpenParsh implements OpenBlocksModule.ProjectParser {

    WeakReference<Context> context;
    final String version = "1.0";

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

    // Shouldn't this be determined by the editor modules?
    @Override
    public Pair<OpenBlocksCode, OpenBlocksLayout> initializeEmptyProject() {
        return null;
    }

    // TODO: 3/15/21 Make a more complex structure that store different stuff like
    //  "this activity" to improve efficiency in reading these files

    // So the project raw structure will be simply something like this
    /* PROJECT_ID
     * L metadata -> Contains project metadata
     * L layout -> Contains OpenBlocksLayout
     * L code -> Contains OpenBlocksCode
     */

    @Override
    public OpenBlocksLayout parseLayout(OpenBlocksRawProject project) {
        OpenBlocksFile layout_file = null;

        for (OpenBlocksFile file : project.files) {
            if (file.name.equals("layout")) {
                layout_file = file;

                break;
            }
        }

        if (layout_file == null) {
            throw new RuntimeException("Parsing failed, layout file cannot be found.");
        }

        InputStream stream = new ByteArrayInputStream(layout_file.data);


        // TODO: 3/15/21 This

        return null;
    }

    @Override
    public OpenBlocksCode parseCode(OpenBlocksRawProject project) {
        return null;
    }

    @Override
    public OpenBlocksProjectMetadata parseMetadata(OpenBlocksRawProject project) {
        OpenBlocksFile metadata_file = null;

        for (OpenBlocksFile file : project.files) {
            if (file.name.equals("metadata")) {
                metadata_file = file;

                break;
            }
        }

        if (metadata_file == null) {
            throw new RuntimeException("Parsing failed, metadata file cannot be found.");
        }

        ByteBuffer metadata_data = ByteBuffer.wrap(metadata_file.data);

        // TODO: 3/15/21 this 

        return null;
    }

    @Override
    public OpenBlocksRawProject saveProject(OpenBlocksProjectMetadata metadata, OpenBlocksCode code, OpenBlocksLayout layout) {
        OpenBlocksRawProject rawProject = new OpenBlocksRawProject();
        rawProject.files = new ArrayList<>();

        // Add a file used to indicate the version
        rawProject.files.add(new OpenBlocksFile(version.getBytes(), "openparsh-ver"));

        // =========================================================================================
        ByteBuffer metadata_buff = ByteBuffer.wrap(
                new byte[
                    metadata.getName().length() +
                    metadata.getPackageName().length() +
                    metadata.getVersionName().length() +
                    1024 /* This 1024 is just to make sure everything shouldn't pass the limit */
                ]
        );

        // Simply {name}.{package}.{version_name}.version_code
        metadata_buff.put(metadata.getName().getBytes());
        metadata_buff.put(metadata.getPackageName().getBytes());
        metadata_buff.put(metadata.getVersionName().getBytes());
        metadata_buff.putInt(metadata.getVersionCode());

        rawProject.files.add(new OpenBlocksFile(metadata_buff.array(), "metadata"));
        // =========================================================================================


        // =========================================================================================
        ByteBuffer code_buff = ByteBuffer.wrap(new byte[0]);

        // TODO: 3/15/21 this

        rawProject.files.add(new OpenBlocksFile(code_buff.array(), "code"));
        // =========================================================================================


        // =========================================================================================
        ByteBuffer layout_buff = ByteBuffer.wrap(new byte[10]);

        // TODO: 3/15/21 this too

        rawProject.files.add(new OpenBlocksFile(layout_buff.array(), "layout"));
        // =========================================================================================

        return rawProject;
    }
}
