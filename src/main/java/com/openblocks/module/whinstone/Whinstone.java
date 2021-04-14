package com.openblocks.module.whinstone;

import android.content.Context;

import com.openblocks.moduleinterface.OpenBlocksModule;
import com.openblocks.moduleinterface.callbacks.Logger;
import com.openblocks.moduleinterface.exceptions.ParseException;
import com.openblocks.moduleinterface.models.OpenBlocksFile;
import com.openblocks.moduleinterface.models.OpenBlocksProjectMetadata;
import com.openblocks.moduleinterface.models.OpenBlocksRawProject;
import com.openblocks.moduleinterface.models.code.BlockCode;
import com.openblocks.moduleinterface.models.config.OpenBlocksConfig;
import com.openblocks.moduleinterface.models.layout.LayoutViewXMLAttribute;
import com.openblocks.moduleinterface.projectfiles.OpenBlocksCode;
import com.openblocks.moduleinterface.projectfiles.OpenBlocksLayout;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Whinstone is meant to be an efficient parser where it converts both code and layout into
 * a byte-level format, where it will save some storage space
 */
public class Whinstone implements OpenBlocksModule.ProjectParser {

    WeakReference<Context> context;
    final String version = "1.0";

    @Override
    public Type getType() {
        return Type.PROJECT_PARSER;
    }

    @Override
    public void initialize(Context context, Logger logger) {
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

    // TODO: 3/15/21 Make a more complex structure that store different stuff like
    //  "this activity" to improve efficiency in reading these files

    // So the project raw structure will be simply something like this
    /* PROJECT_ID
     * L metadata -> Contains project metadata
     * L layout -> Contains OpenBlocksLayout
     * L code -> Contains OpenBlocksCode
     */

    @Override
    @NonNull
    public OpenBlocksLayout parseLayout(OpenBlocksRawProject project) throws ParseException {
        OpenBlocksFile layout_file = null;

        for (OpenBlocksFile file : project.files) {
            if (file.name.equals("layout")) {
                layout_file = file;

                break;
            }
        }

        if (layout_file == null) {
            throw new ParseException("Parsing failed, layout file cannot be found.");
        }

        return parseLayout(new String(layout_file.data, StandardCharsets.UTF_8));
    }

    @Override
    @NonNull
    public OpenBlocksCode parseCode(OpenBlocksRawProject project) throws ParseException {
        OpenBlocksFile code_file = null;

        for (OpenBlocksFile file : project.files) {
            if (file.name.equals("code")) {
                code_file = file;

                break;
            }
        }

        if (code_file == null) {
            throw new ParseException("Parsing failed, code file cannot be found.");
        }

        return parseCode(new String(code_file.data, StandardCharsets.UTF_8));
    }

    @Override
    @NonNull
    public OpenBlocksProjectMetadata parseMetadata(OpenBlocksRawProject project) throws ParseException {
        OpenBlocksFile metadata_file = null;

        for (OpenBlocksFile file : project.files) {
            if (file.name.equals("metadata")) {
                metadata_file = file;

                break;
            }
        }

        if (metadata_file == null) {
            throw new ParseException("Parsing failed, metadata file cannot be found.");
        }

        StringBuilder buffer = new StringBuilder();
        ArrayList<String> metadata_split = new ArrayList<>();
        int data_index = 0;

        int version_code = 0;

        // This data file contains
        // index:    0       1           2              3
        //        {name}.{package}.{version_name}.{version_code}
        for (byte current_byte: metadata_file.data) {
            // If the current byte isn't a separator
            if (current_byte != 0x0) {
                if (data_index == 3) {
                    version_code = current_byte;

                    break;
                } else {
                    buffer.append(current_byte);
                }
            } else {
                // Add the buffer
                metadata_split.add(buffer.toString());

                // Empty buffer
                buffer = new StringBuilder();

                data_index++;
            }
        }

        return new OpenBlocksProjectMetadata(
                metadata_split.get(0),
                metadata_split.get(1),
                metadata_split.get(2),
                version_code
        );
    }

    @Override
    @NonNull
    public OpenBlocksRawProject saveProject(OpenBlocksProjectMetadata metadata, OpenBlocksCode code, OpenBlocksLayout layout) {
        OpenBlocksRawProject rawProject = new OpenBlocksRawProject();
        rawProject.files = new ArrayList<>();

        // Add a file used to indicate the version
        rawProject.files.add(new OpenBlocksFile(version.getBytes(), "whinstone-ver"));

        // =========================================================================================

        // Simply {name}.{package}.{version_name}.{version_code}
        String metadata_ser =
                metadata.getName() +
                0x0 +
                metadata.getPackageName() +
                0x0 +
                metadata.getVersionName() +
                0x0 +
                metadata.getVersionCode();

        rawProject.files.add(new OpenBlocksFile(metadata_ser.getBytes(), "metadata"));
        // =========================================================================================


        // =========================================================================================
        String code_serialized = serializeCode(code);

        rawProject.files.add(new OpenBlocksFile(code_serialized.getBytes(), "code"));
        // =========================================================================================


        // =========================================================================================
        String layout_serialized = serializeLayout(layout, 0);

        rawProject.files.add(new OpenBlocksFile(layout_serialized.getBytes(), "layout"));
        // =========================================================================================

        return rawProject;
    }

    /* Serialized layout should be something like this
     *
     * Note: Line breaks doesn't count
     *
     * LinearLayout
     * 0x11                             <- used to separate xml attributes
     * android.id.+@/linearLayout1
     * 0x11
     * android.layout_width.match_parent
     * 0x11
     * android.layout_height.match_parent
     * 0x11
     * etc..
     * 0x22                            <- Opening bracket for our childs
     *
     *   LinearLayout                  <- Child of the above view
     *   0x11
     *   android.id.+@/linearLayout2
     *   0x11
     *   android.layout_width.match_parent
     *   0x11
     *   android.layout_width.wrap_content
     *   etc..
     *   0x22                          <- Empty childs
     *   0x33
     *
     *   LinearLayout
     *   etc etc..
     *
     * 0x33                            <- Closing bracket for childs
     */

    /**
     * This function serializes an {@link OpenBlocksLayout} into a binary data
     * @param layoutView The layout that is to be serialized
     * @param depth The depth of this layout (as a child), should be 0 when you call this
     * @return The serialized layout
     */
    private String serializeLayout(OpenBlocksLayout layoutView, int depth) {
        // Shouldn't be using StringBuilder for this, but this is all I know
        StringBuilder out = new StringBuilder();

        out.append(layoutView.view_name); // View name

        // Serialize xml attributes
        // TODO: 3/18/21 Optimize this by grouping each attributes depending on their prefix
        for (LayoutViewXMLAttribute xml_attribute : layoutView.xml_attributes) {
            out.append(0x11); // This hex is used to separate each attributes
            out.append(xml_attribute.prefix);
            out.append(0x00);
            out.append(xml_attribute.attribute_name);
            out.append(0x00);
            out.append(xml_attribute.value);
        }

        // Open the bracket for our childs
        out.append(0x22);

        // Check if this view has any childs
        if (layoutView.childs.size() != 0) {
            // K, serialize those childs
            StringBuilder childs = new StringBuilder();

            // Recursively call ourself
            for (OpenBlocksLayout child: layoutView.childs) {
                String serialized_child = serializeLayout(child, depth + 1);

                childs.append(serialized_child);
            }

            // Then write the childs
            out.append(childs);
        }

        // Close the "bracket"
        out.append(0x33);

        return out.toString();
    }

    private OpenBlocksLayout parseLayout(String serialized) throws ParseException {
        StringBuilder buffer = new StringBuilder();

        // ArrayList of Integer: substack, OpenBlocksLayout: view
        ArrayList<OpenBlocksLayout> childs = new ArrayList<>();

        String view_type = "";
        ArrayList<LayoutViewXMLAttribute> attributes = new ArrayList<>();

        int argument_counter = 0;

        int attribute_counter = 0;
        String attribute_prefix = "";
        String attribute_name = "";
        String attribute_value;

        boolean inside_childs = false;
        boolean reading_attribute = false;

        boolean was_closing_child = false;

        for (byte b : serialized.getBytes()) {

            if (reading_attribute) {

                // If the current byte is not a separator
                if (b != 0x0) {
                    buffer.append((char) b);

                } else {
                    // This is 0x0, the separator

                    if (attribute_counter == 0) {
                        // First item (prefix or namespace)
                        attribute_prefix = buffer.toString();

                    } else if (attribute_counter == 1) {
                        // Second item, The attribute name
                        attribute_name = buffer.toString();

                    } else if (attribute_counter == 2) {
                        // Third item, The attribute value
                        attribute_value = buffer.toString();

                        // Create the attribute
                        attributes.add(
                                new LayoutViewXMLAttribute(
                                        attribute_prefix,
                                        attribute_name,
                                        attribute_value
                                )
                        );

                        // In this point, we're done parsing the attribute
                        reading_attribute = false;
                    }

                    // Reset our buffer
                    buffer = new StringBuilder();

                    // Increase our attribute counter
                    attribute_counter++;
                }

                // Don't need to go through the loop, just skip em
                continue;
            }

            if (inside_childs) {
                buffer.append((char) b);

                if (b == 0x33) {

                    // If it was a closing child and we got closing child, means this is the closing of the parent / e n d   o f   t h i s   v i e w
                    if (was_closing_child) {
                        // END OF THIS VIEW!!!
                        return new OpenBlocksLayout(childs, view_type, attributes);

                    } else {
                        // This indicates the end of a child's view
                        // Now recursively call ourselves to serialize our child
                        childs.add(parseLayout(buffer.toString()));

                        was_closing_child = true;
                        continue;
                    }
                }

                was_closing_child = false;

                continue;
            }

            // Is this an attribute separator?
            if (b == 0x11) {
                if (argument_counter == 0) {
                    // If the argument counter kicks in, means that we're on the end of the view type
                    view_type = buffer.toString();

                    // Clear the buffer
                    buffer = new StringBuilder();
                } else if (argument_counter > 0) {
                    reading_attribute = true;

                    // Clear the buffer
                    buffer = new StringBuilder();
                }

                argument_counter++;

            // Is this the childs?
            } else if (b == 0x22) {
                inside_childs = true;

                buffer = new StringBuilder();
            } else {
                buffer.append((char) b);
            }

        }

        // Failed to parse
        throw new ParseException("Failed to parse, loop is skipped, Is the layout empty / corrupted?");
    }


    /* Serialized code should be something like this
     *
     * Note: Line breaks doesn't count
     *
     * 0x11                 <- this is the separator for code templates
     * opcode %s.showMessage("%s");
     * 0x11
     * opcode2 %var = %n + %n.%var = %n + %n
     * etc..
     * 0x22                 <- this is the separator for blocks
     * 0                    <- code template index
     * .Hello World         <- Value as the argument, you can create multiple argument by adding null before each arguments
     * 0x22
     * 4
     * .10
     * .Hello World2
     */

    // TODO: 3/20/21 Add nested blocks support

    private String serializeCode(OpenBlocksCode code) {
        // Shouldn't be using StringBuilder for this, but this is all I know
        StringBuilder out = new StringBuilder();

        ArrayList<String> template_keys = new ArrayList<>(code.code_templates.keySet());

        // Serialize the code templates
        for (String key: template_keys) {
            out.append(0x11);
            out.append(key);
            out.append(0x0);
            out.append(code.code_templates.get(key));
        }

        // Serialize blocks
        for (BlockCode block : code.blocks) {
            out.append(0x22);
            out.append(template_keys.indexOf(block.opcode));

            for (String parameter : block.parameters) {
                out.append(0x0);
                out.append(parameter);
            }
        }

        return out.toString();
    }

    private enum ParseCodeMode {
        CODE_TEMPLATE,
        BLOCK
    }
    
    private OpenBlocksCode parseCode(String serialized) {
        ParseCodeMode current_mode = null;

        HashMap<String, String> code_template = new HashMap<>();
        ArrayList<String> code_template_keys = new ArrayList<>();
        ArrayList<BlockCode> blocks = new ArrayList<>();

        // This is the buffer for everything, but make sure to empty it after you use it
        StringBuilder buffer = new StringBuilder();

        // code template ===
        String opcode = "";
        // code template ===

        // blocks ===
        ArrayList<String> arguments = new ArrayList<>();
        int template_index = 0;
        // blocks ===

        // This counter is just going to be used to count, nothing else
        int data_index = 0;

        for (byte c : serialized.getBytes()) {
            if (c == 0x11) {
                current_mode = ParseCodeMode.CODE_TEMPLATE;

                data_index = 0;
            } else if (c == 0x22) {
                // If the previous mode was a block, we should save the block into the blocks array list
                if (current_mode == ParseCodeMode.BLOCK) {
                    blocks.add(new BlockCode(code_template.get(code_template_keys.get(template_index)), arguments));
                }

                current_mode = ParseCodeMode.BLOCK;

                data_index = 0;
            }

            ////////////////////////////////////////////////////////////////////////////////////////

            if (current_mode == ParseCodeMode.CODE_TEMPLATE) {
                if (c == 0x0) {
                    if (data_index == 1) {
                        code_template.put(opcode, buffer.toString());
                        code_template_keys.add(opcode);
                    } else {
                        opcode = buffer.toString();

                        data_index++;
                    }

                    buffer = new StringBuilder();

                } else {
                    buffer.append(c);
                }

            ////////////////////////////////////////////////////////////////////////////////////////

            } else if (current_mode == ParseCodeMode.BLOCK) {
                if (c == 0x0) {
                    if (data_index == 1) {
                        // This is the code template index
                        template_index = Integer.parseInt(buffer.toString());

                        // Empty the buffer
                        buffer = new StringBuilder();
                    } else if (data_index > 1) {
                        // This is an argument
                        arguments.add(buffer.toString());

                        // Empty the buffer
                        buffer = new StringBuilder();
                    }

                    data_index++;
                } else {
                    buffer.append(c);
                }
            }
        }

        return new OpenBlocksCode(code_template, blocks);
    }
}
