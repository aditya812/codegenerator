package com.yourorganization.maven_sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.app.sample.domain.InsuranceClaim;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
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
public class CodeGenerator {
	
	private static Workbook mappingWorkbook;
	
	private static String destinationPath = "C:\\Users\\vmodhugu\\Desktop\\Innovation_code_generator\\jhipster\\App1\\App2\\App4\\";
	
	public CodeGenerator()throws Exception {
		mappingWorkbook = new XSSFWorkbook(new FileInputStream(CodeGenerator.class.getClassLoader().getResource("mapper.xlsx").getFile()));
	}
	
	public static void generateCode() throws Exception {
		Workbook workbook = new XSSFWorkbook(new FileInputStream(CodeGenerator.class.getClassLoader().getResource("Input.xlsx").getFile()));
        Sheet datatypeSheet = workbook.getSheetAt(0);
        Iterator<Row> iterator = datatypeSheet.iterator();
        NodeList<Statement> nodeList = iterateInputRequestAndGenerateCode(iterator);
        workbook.close();
        createClassAndInsertMethodBody(nodeList);
        
	}
	
	private static NodeList<Statement> iterateInputRequestAndGenerateCode(Iterator<Row> iterator)throws Exception{
		
		NodeList<Statement> nodeList = new NodeList<>();
        //to skip the first header row
        iterator.next();
        while (iterator.hasNext()) {
            Row currentRow = iterator.next();
            if(currentRow.getCell(0) == null || currentRow.getCell(0).toString().trim().equals(""))
            	break;
            
            IfStmt ifStmt = new IfStmt();
            
            String ifConditionField = currentRow.getCell(1).toString();
            String codeToAccessField = getCodeForBussinessField(ifConditionField, false);
            
            NameExpr insuranceClaim = new NameExpr("insuranceClaim");
            FieldAccessExpr ifField = new FieldAccessExpr(insuranceClaim, codeToAccessField);
            if(currentRow.getCell(2).toString().equals("equals")) {
            	MethodCallExpr conditionExpr = new MethodCallExpr(ifField, "equals");
            	String ifConditionValue = "";
            	//field is static
            	if(currentRow.getCell(3) != null && !currentRow.getCell(3).toString().trim().equals("")) {
            		ifConditionValue = currentRow.getCell(3).toString();
            		conditionExpr.addArgument(new StringLiteralExpr(ifConditionValue));
            	}else {
            		ifConditionValue = getCodeForBussinessField(currentRow.getCell(4).toString(), false);
            		conditionExpr.addArgument(new NameExpr(ifConditionValue));
            	}
            	ifStmt.setCondition(conditionExpr);
            }
            
            String thenFieldName = currentRow.getCell(6).toString();
            String thenFieldValue = "";
            String thenFieldSetterCode = getCodeForBussinessField(thenFieldName, true);
            FieldAccessExpr thenField = new FieldAccessExpr(insuranceClaim, thenFieldSetterCode);
//            String[] fields = thenFieldSetterCode.split("\\.");
            MethodCallExpr thenSetterExpr = new MethodCallExpr();
            thenSetterExpr.setName(thenField.toString());
            if(currentRow.getCell(8) != null && !currentRow.getCell(8).toString().trim().equals("")) {
            	thenFieldValue = currentRow.getCell(8).toString();
            	thenSetterExpr.addArgument(new StringLiteralExpr(thenFieldValue));
        	}else {
        		thenFieldValue = getCodeForBussinessField(currentRow.getCell(9).toString(), false);
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
	
	private static String getCodeForBussinessField(String bussinessField, boolean isSetterMethod)throws Exception {
		Sheet datatypeSheet = mappingWorkbook.getSheetAt(0);
        Iterator<Row> iterator = datatypeSheet.iterator();
        String hirerchyValue = "NotFound";
        while (iterator.hasNext()) {
            Row currentRow = iterator.next();
            if(currentRow.getCell(0).toString().contains(bussinessField)) {
            	hirerchyValue = currentRow.getCell(1).toString();
            	break;
            }
            
        }
        if(hirerchyValue.equals("NotFound")) 
        	throw new Exception("'"+bussinessField + "' Not found in the mapping");
        if(!isSetterMethod)
        	return getJavaCodeToAccessField(hirerchyValue);
        return getJavaCodeToSetField(hirerchyValue);
	}
	
	private static String getJavaCodeToAccessField(String fieldHirerchy) {
		StringBuilder statementCode = new StringBuilder();
		String[] fields = fieldHirerchy.split("\\.");
		for(int i=1; i < fields.length; i++) {
			statementCode.append(".get"+fields[i].substring(0, 1).toUpperCase() + fields[i].substring(1)+"()");
		}
		return statementCode.toString().substring(1);
	}
	
	private static String getJavaCodeToSetField(String fieldHirerchy) {
		StringBuilder statementCode = new StringBuilder();
		String[] fields = fieldHirerchy.split("\\.");
		for(int i=1; i < fields.length; i++) {
			if(i < fields.length - 1)
				statementCode.append(".get"+fields[i].substring(0, 1).toUpperCase() + fields[i].substring(1)+"()");
			else 
				statementCode.append(".set"+fields[i].substring(0, 1).toUpperCase() + fields[i].substring(1));
		}
		
		return statementCode.toString().substring(1);
	}
	
	public static void main(String[] args) throws Exception {
//		cd C:\Users\addade\Desktop\Work\Innovation\sample4
//
		File file = new File(destinationPath+".yo-rc.json");
		File yo_file = new File(CodeGenerator.class.getClassLoader().getResource(".yo-rc.json").getFile());
		copyFile(yo_file, file);
		
//		Process p1 = Runtime.getRuntime().exec("copy "+yo_file+" .", null, new File(destinationPath));
//		p1.waitFor();
		Process p2 = Runtime.getRuntime().exec("cmd /c start /wait jhipster", null, new File(destinationPath));
		
		p2.waitFor(); 
		/*CodeGenerator codeGenerator = */new CodeGenerator();
		generateCode();
		Process p3 = Runtime.getRuntime().exec("cmd /c start /wait set CLASSPATH=C:\\Users\\vmodhugu\\Documents\\My Received Files\\claim.jar & mvn clean install", null, new File(destinationPath));
		p3.waitFor();
		Process p4 = Runtime.getRuntime().exec("cmd /c start /wait java -jar target\\PricingMapping.war", null, new File(destinationPath));
		p4.waitFor();
		
	}
    
	private static void createClassAndInsertMethodBody(NodeList<Statement> methodBodyStatements)throws Exception {
		CompilationUnit cu = new CompilationUnit();
		String packageName = "com.app.sample.service";
		String className = "InsuranceClaimMapper";
        cu.setPackageDeclaration(packageName);
        ClassOrInterfaceDeclaration type = cu.addClass("InsuranceClaimMapper");

        MethodDeclaration mapInsuranceClaim = type.addMethod("mapInsuranceClaim", Modifier.Keyword.PUBLIC);

        mapInsuranceClaim.addAndGetParameter(InsuranceClaim.class, "insuranceClaim");
        mapInsuranceClaim.setType(InsuranceClaim.class);
        
        mapInsuranceClaim.setBody(new BlockStmt(methodBodyStatements));
        
        PrintWriter writer = new PrintWriter(destinationPath+"src\\main\\java\\"+packageName.replaceAll("\\.", "\\\\")+"\\"+className+".java");
        writer.print(cu.toString());
        writer.close();
	}
    
	public static void copyFile(File source, File destination)throws Exception {
//		if(!destination.exists())
//			destination.mkdirs();
//		

		createFile(destination);
		
		InputStream inStream = null;
		OutputStream outStream = null;

		try {

			inStream = new FileInputStream(source);
			outStream = new FileOutputStream(destination);

			byte[] buffer = new byte[1024];

			int length;
			// copy the file content in bytes
			while ((length = inStream.read(buffer)) > 0) {

				outStream.write(buffer, 0, length);

			}

			inStream.close();
			outStream.close();

			System.out.println("File is copied successful!");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void createFile(File file) throws Exception{
		
	        if (!file.exists()) {
	        	file.getParentFile().mkdirs();
	        	FileWriter writer = new FileWriter(file);
	        } 
	}
}
