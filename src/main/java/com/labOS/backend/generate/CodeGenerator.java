package com.labOS.backend.generate;

import cn.hutool.core.io.FileUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.io.FileWriter;
import java.io.Writer;

/**
 * Code generator
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * 
 */
public class CodeGenerator {

    /**
     * Usage: Modify generation parameters and paths, comment out unnecessary generation logic, then run
     *
     * @param args
     * @throws TemplateException
     * @throws IOException
     */
    public static void main(String[] args) throws TemplateException, IOException {
        // Specify generation parameters
        String packageName = "com.labOS.backend";
        String dataName = "User Comment";
        String dataKey = "userComment";
        String upperDataKey = "UserComment";

        // Encapsulate generation parameters
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("packageName", packageName);
        dataModel.put("dataName", dataName);
        dataModel.put("dataKey", dataKey);
        dataModel.put("upperDataKey", upperDataKey);

        // Default generation path
        String projectPath = System.getProperty("user.dir");
        // Reference path, you can adjust the outputPath below
        String inputPath = projectPath + File.separator + "src/main/resources/templates/TemplateName.java.ftl";
        String outputPath = String.format("%s/generator/package/%sSuffix.java", projectPath, upperDataKey);

        // 1. Generate Controller
        // Specify generation path
        inputPath = projectPath + File.separator + "src/main/resources/templates/TemplateController.java.ftl";
        outputPath = String.format("%s/generator/controller/%sController.java", projectPath, upperDataKey);
        // Generate
        doGenerate(inputPath, outputPath, dataModel);
        System.out.println("Controller generated successfully, file path: " + outputPath);

        // 2. Generate Service interface and implementation
        // Generate Service interface
        inputPath = projectPath + File.separator + "src/main/resources/templates/TemplateService.java.ftl";
        outputPath = String.format("%s/generator/service/%sService.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        System.out.println("Service interface generated successfully, file path: " + outputPath);
        // Generate Service implementation
        inputPath = projectPath + File.separator + "src/main/resources/templates/TemplateServiceImpl.java.ftl";
        outputPath = String.format("%s/generator/service/impl/%sServiceImpl.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        System.out.println("Service implementation generated successfully, file path: " + outputPath);

        // 3. Generate data model classes (including DTO and VO)
        // Generate DTO
        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateAddRequest.java.ftl";
        outputPath = String.format("%s/generator/model/dto/%sAddRequest.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateQueryRequest.java.ftl";
        outputPath = String.format("%s/generator/model/dto/%sQueryRequest.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateEditRequest.java.ftl";
        outputPath = String.format("%s/generator/model/dto/%sEditRequest.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateUpdateRequest.java.ftl";
        outputPath = String.format("%s/generator/model/dto/%sUpdateRequest.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        System.out.println("DTO generated successfully, file path: " + outputPath);
        // Generate VO
        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateVO.java.ftl";
        outputPath = String.format("%s/generator/model/vo/%sVO.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        System.out.println("VO generated successfully, file path: " + outputPath);
    }

    /**
     * Generate file
     *
     * @param inputPath  Template file input path
     * @param outputPath Output path
     * @param model      Data model
     * @throws IOException
     * @throws TemplateException
     */
    public static void doGenerate(String inputPath, String outputPath, Object model) throws IOException, TemplateException {
        // Create Configuration object with FreeMarker version number
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_31);

        // Specify template file directory
        File templateDir = new File(inputPath).getParentFile();
        configuration.setDirectoryForTemplateLoading(templateDir);

        // Set character encoding for template files
        configuration.setDefaultEncoding("utf-8");

        // Create template object and load specified template
        String templateName = new File(inputPath).getName();
        Template template = configuration.getTemplate(templateName);

        // Create file and parent directory if not exists
        if (!FileUtil.exist(outputPath)) {
            FileUtil.touch(outputPath);
        }

        // Generate
        Writer out = new FileWriter(outputPath);
        template.process(model, out);

        // Don't forget to close after generating file
        out.close();
    }
}
