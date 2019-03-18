package com.deloitte.code.generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

/**
 * Some code that uses JavaParser.
 */
public class CodeGeneratorFromExcel {

	private static Workbook mappingWorkbook;

	private static String destinationPath = "";

	private static String inputFile = "";

	private static String basePackageName = "com.app.sample";

	public CodeGeneratorFromExcel() throws Exception {
		mappingWorkbook = new XSSFWorkbook(getClass().getClassLoader().getResourceAsStream("mapper.xlsx"));
	}

	public static void generateCode() throws Exception {
		Workbook workbook = new XSSFWorkbook(new FileInputStream(inputFile));
		Sheet datatypeSheet = workbook.getSheetAt(0);
		Iterator<Row> iterator = datatypeSheet.iterator();
		NodeList<Statement> nodeList = iterateInputRequestAndGenerateCode(iterator);
		/*{  
		      "ruleId":"1001",
		      "conditions":{  
		         "cond":[  
		            {  
		               "operatorValue":"=",
		               "ifValue":"(Claim) Total Allowed Amount",
		               "setvalue":"0",
					   "isStatic":true
		            },
		            {  
		               "operatorValue":"=",
		               "ifValue":"(Claim) Total Allowed Amount",
		               "setvalue":"0",
					   "isStatic":true
		            }
		         ],
		         "conditionOp":""
		      },
		      
		   }*/
		JSONParser parser = new JSONParser();
		JSONArray jsonArray = (JSONArray) parser.parse(new FileReader("C:\\Users\\vmodhugu\\Desktop\\Innovation_code_generator\\InputJson.json"));
		for(Object obj : jsonArray) {
			JSONObject jsonObj = (JSONObject) obj;
			JSONObject conditions = (JSONObject) jsonObj.get("conditions");
			JSONObject cond1 = (JSONObject) ((JSONArray) conditions.get("cond")).get(0);
			String operatorValue = cond1.get("operatorValue").toString();
			String ifValue = cond1.get("ifValue").toString();
			String setvalue = cond1.get("setvalue").toString();
			Boolean isStatic = Boolean.valueOf(cond1.get("isStatic").toString());
			
			
		}
		
		
		workbook.close();
		createInsuranceMapperClass(nodeList);
		createControllerClass();
	}

	private static NodeList<Statement> iterateInputRequestAndGenerateCode(Iterator<Row> iterator) throws Exception {

		NodeList<Statement> nodeList = new NodeList<>();
		// to skip the first header row
		iterator.next();
		while (iterator.hasNext()) {
			Row currentRow = iterator.next();
			if (currentRow.getCell(0) == null || currentRow.getCell(0).toString().trim().equals(""))
				break;

			IfStmt ifStmt = new IfStmt();

			String ifConditionField = currentRow.getCell(1).toString();
			String codeToAccessField = getCodeForBussinessField(ifConditionField, false);

			NameExpr insuranceClaim = new NameExpr("insuranceClaim");
			FieldAccessExpr ifField = new FieldAccessExpr(insuranceClaim, codeToAccessField);
			if (currentRow.getCell(2).toString().equals("equals")) {
				MethodCallExpr conditionExpr = new MethodCallExpr(ifField, "equals");
				String ifConditionValue = "";
				// field is static
				if (currentRow.getCell(3) != null && !currentRow.getCell(3).toString().trim().equals("")) {
					ifConditionValue = currentRow.getCell(3).toString();
					conditionExpr.addArgument(new StringLiteralExpr(ifConditionValue));
				} else {
					ifConditionValue = getCodeForBussinessField(currentRow.getCell(4).toString(), false);
					conditionExpr.addArgument(new NameExpr(ifConditionValue));
				}
				ifStmt.setCondition(conditionExpr);
			}

			String thenFieldName = currentRow.getCell(6).toString();
			String thenFieldValue = "";
			String thenFieldSetterCode = getCodeForBussinessField(thenFieldName, true);
			FieldAccessExpr thenField = new FieldAccessExpr(insuranceClaim, thenFieldSetterCode);
			// String[] fields = thenFieldSetterCode.split("\\.");
			MethodCallExpr thenSetterExpr = new MethodCallExpr();
			thenSetterExpr.setName(thenField.toString());
			if (currentRow.getCell(8) != null && !currentRow.getCell(8).toString().trim().equals("")) {
				thenFieldValue = currentRow.getCell(8).toString();
				thenSetterExpr.addArgument(new StringLiteralExpr(thenFieldValue));
			} else {
				thenFieldValue = getCodeForBussinessField(currentRow.getCell(9).toString(), false);
				thenFieldValue = insuranceClaim + "." + thenFieldValue;
				thenSetterExpr.addArgument(new NameExpr(thenFieldValue));
			}
			BlockStmt thenBlock = new BlockStmt();
			NodeList<Statement> thenBlockStatements = new NodeList<>();
			thenBlockStatements.add(new ExpressionStmt(thenSetterExpr));
			thenBlock.setStatements(thenBlockStatements);
			ifStmt.setThenStmt(thenBlock);

			nodeList.add(ifStmt);
		}
		nodeList.add(new ExpressionStmt(new NameExpr("return insuranceClaim")));
		return nodeList;
	}

	private static String getCodeForBussinessField(String bussinessField, boolean isSetterMethod) throws Exception {
		Sheet datatypeSheet = mappingWorkbook.getSheetAt(0);
		Iterator<Row> iterator = datatypeSheet.iterator();
		String hirerchyValue = "NotFound";
		while (iterator.hasNext()) {
			Row currentRow = iterator.next();
			if (currentRow.getCell(0).toString().contains(bussinessField)) {
				hirerchyValue = currentRow.getCell(1).toString();
				break;
			}

		}
		if (hirerchyValue.equals("NotFound"))
			throw new Exception("'" + bussinessField + "' Not found in the mapping");
		if (!isSetterMethod)
			return getJavaCodeToAccessField(hirerchyValue);
		return getJavaCodeToSetField(hirerchyValue);
	}

	private static String getJavaCodeToAccessField(String fieldHirerchy) {
		StringBuilder statementCode = new StringBuilder();
		String[] fields = fieldHirerchy.split("\\.");
		for (int i = 1; i < fields.length; i++) {
			statementCode.append(".get" + fields[i].substring(0, 1).toUpperCase() + fields[i].substring(1) + "()");
		}
		return statementCode.toString().substring(1);
	}

	private static String getJavaCodeToSetField(String fieldHirerchy) {
		StringBuilder statementCode = new StringBuilder();
		String[] fields = fieldHirerchy.split("\\.");
		for (int i = 1; i < fields.length; i++) {
			if (i < fields.length - 1)
				statementCode.append(".get" + fields[i].substring(0, 1).toUpperCase() + fields[i].substring(1) + "()");
			else
				statementCode.append(".set" + fields[i].substring(0, 1).toUpperCase() + fields[i].substring(1));
		}

		return statementCode.toString().substring(1);
	}

	public static void main1(String[] args) throws Exception {

		inputFile = args[0];
		destinationPath = args[1];
		destinationPath = destinationPath.endsWith("\\") ? destinationPath : destinationPath + "\\";

		File yo_file = new File(destinationPath + ".yo-rc.json");
		File sample_file = new File(destinationPath + "sample.jh");
		new CodeGeneratorFromExcel();

		// copy yo-ro file
		InputStream yo_file_stream = CodeGeneratorFromExcel.class.getClassLoader().getResourceAsStream(".yo-rc.json");
		copyFile(yo_file_stream, yo_file);

		// copy sample.jh file
		InputStream sample_file_stream = CodeGeneratorFromExcel.class.getClassLoader().getResourceAsStream("sample.jh");
		copyFile(sample_file_stream, sample_file);

		ProcessBuilder builder1 = new ProcessBuilder("cmd.exe", "/c",
				"cd " + destinationPath + " && jhipster import-jdl sample.jh");
		System.out.println(
				"====================Execution of entity java source files creation from entityObjects.jh started...====================");
		executeCommands(builder1);
		System.out.println("====================Entity source files creation completed!====================");

		ProcessBuilder builder2 = new ProcessBuilder("cmd.exe", "/c", "cd " + destinationPath + " && jhipster");
		System.out.println(
				"====================Execution of Project skelton source files creation from .yo-rc.json started...====================");
		executeCommands(builder2);
		System.out.println("====================Project slelton source files creation completed!====================");

		System.out.println(
				"====================Execution of generating the java mapping source files started...====================");
		generateCode();
		System.out
				.println("====================Generation of java mapping source files completed !====================");

		ProcessBuilder builder3 = new ProcessBuilder("cmd.exe", "/c",
				"cd " + destinationPath + " && mvn clean install -DskipTests");
		System.out.println(
				"====================Execution for trigerring the 'Maven build' to build the generated project sources started...====================");
		executeCommands(builder3);
		System.out.println("'Mavnen Build' execution completed!");

		ProcessBuilder builder4 = new ProcessBuilder("cmd.exe", "/c",
				"cd " + destinationPath + " && java -jar target\\sample-0.0.1-SNAPSHOT.war");
		System.out.println(
				"====================Execution to start the java microservice has started...====================");
		executeCommands(builder4);

	}

	private static void executeCommands(ProcessBuilder builder) throws Exception {
		builder.redirectErrorStream(true);
		Process p = builder.start();
		BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while (true) {
			line = r.readLine();
			if (line == null) {
				break;
			}
			System.out.println(line);
		}
		p.waitFor();
	}

	private static void createInsuranceMapperClass(NodeList<Statement> methodBodyStatements) throws Exception {
		CompilationUnit cu = new CompilationUnit();

		String className = "InsuranceClaimMapper";
		cu.setPackageDeclaration(basePackageName + ".service");
		ClassOrInterfaceDeclaration type = cu.addClass("InsuranceClaimMapper");

		MethodDeclaration mapInsuranceClaim = type.addMethod("mapInsuranceClaim", Modifier.Keyword.PUBLIC);

		cu.addImport("com.app.sample.domain.InsuranceClaim");
		mapInsuranceClaim.addParameter("InsuranceClaim", "insuranceClaim");
		// mapInsuranceClaim.addAndGetParameter(InsuranceClaim.class, "insuranceClaim");
		mapInsuranceClaim.setType("InsuranceClaim");
		// mapInsuranceClaim.setType(InsuranceClaim.class);

		mapInsuranceClaim.setBody(new BlockStmt(methodBodyStatements));

		PrintWriter writer = new PrintWriter(destinationPath + "src\\main\\java\\"
				+ basePackageName.replaceAll("\\.", "\\\\") + "\\service\\" + className + ".java");
		writer.print(cu.toString());
		writer.close();
	}

	private static void createControllerClass() throws Exception {

		String controllerClassSourcePath = destinationPath + "src\\main\\java\\"
				+ basePackageName.replaceAll("\\.", "\\\\") + "\\web\\rest\\InsuranceClaimResource.java";
		FileInputStream in = new FileInputStream(controllerClassSourcePath);

		JavaParser parser = new JavaParser();
		CompilationUnit cu = parser.parse(in).getResult().get();
		MethodDeclaration method = getMethodByName(cu, "updateInsuranceClaim");
		BlockStmt methodBody = method.getBody().get();
		NodeList<Statement> bodyStatements = new NodeList<>();

		cu.addImport("com.app.sample.service.InsuranceClaimMapper");
		Statement stmt1 = parser.parseStatement("InsuranceClaimMapper mapper = new InsuranceClaimMapper();").getResult()
				.get();
		Statement stmt3 = parser
				.parseStatement(
						"return ResponseEntity.ok().headers(null).body(mapper.mapInsuranceClaim( insuranceClaim));")
				.getResult().get();
		bodyStatements.add(stmt1);

		cu.getTypes().stream().filter(type -> type.getAnnotationByName("RequestMapping").isPresent()).findFirst().get()
				.remove();
		bodyStatements.add(stmt3);
		methodBody.setStatements(bodyStatements);
		cu.toString();

		PrintWriter writer = new PrintWriter(controllerClassSourcePath);
		writer.print(cu.toString());
		writer.close();
	}

	private static MethodDeclaration getMethodByName(CompilationUnit cu, String methodName) {

		List<TypeDeclaration<?>> nodeList = cu.getTypes();
		
		
		for(TypeDeclaration<?> type : nodeList) {
			//temp fix to remove the security annotation
//			type.getAnnotationByName("RequestMapping").ifPresent(c -> { ((SingleMemberAnnotationExpr) c).remove();});
			List<MethodDeclaration> list = type.getMethodsByName(methodName);
			for(Object bd : list) {
				if(bd instanceof MethodDeclaration && ((MethodDeclaration) bd).getName().toString().equals(methodName)) 
					return (MethodDeclaration)bd;
			}
		}
		
		return null;
	}

	public static void copyFile(InputStream source, File destination) throws Exception {

		if (!destination.exists()) {
			destination.getParentFile().mkdirs();
			new FileWriter(destination);
		}

		OutputStream outStream = null;

		try {

			outStream = new FileOutputStream(destination);

			byte[] buffer = new byte[1024];

			int length;
			while ((length = source.read(buffer)) > 0) {

				outStream.write(buffer, 0, length);

			}

			source.close();
			outStream.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
