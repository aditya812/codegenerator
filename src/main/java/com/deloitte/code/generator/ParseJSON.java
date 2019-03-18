package com.deloitte.code.generator;

import java.io.FileReader;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

public class ParseJSON {

	public static NodeList<Statement> parseJson(String inputFile) throws Exception{
		JSONParser jsonParser = new JSONParser();
		
		Object obj = jsonParser.parse(new FileReader(inputFile));
		JSONArray inputArray = new JSONArray(obj.toString());
		
		NodeList<Statement> nodeList = new NodeList<>();
		
		// iterate no. of rules
		for(int i = 0; i < inputArray.length(); i++) {
			JSONObject ruleObject = inputArray.getJSONObject(i);
			parseRuleObject(ruleObject, nodeList);
		}
		nodeList.add(new ExpressionStmt(new NameExpr("return insuranceClaim")));
		return nodeList;
	}
	
	public static NodeList<Statement> parseRuleObject(JSONObject ruleObject, NodeList<Statement> nodeList) throws Exception {
		
		// create an insurance object
		NameExpr insuranceClaim = new NameExpr("insuranceClaim");
		
		IfStmt ifStmt = prepareIfStatementCode(ruleObject, insuranceClaim);
		
		BlockStmt thenStmt = prepareThenStatementCode(ruleObject, insuranceClaim);
		
		ifStmt.setThenStmt(thenStmt);
		
		nodeList.add(ifStmt);
		
		return nodeList;
	}
	
	public static IfStmt prepareIfStatementCode(JSONObject ruleObject, NameExpr insuranceClaim) throws Exception {
		IfStmt ifStmt = new IfStmt();
		BinaryExpr expression = new BinaryExpr();
		
		JSONObject condtions = (JSONObject) ruleObject.get("conditions");
		String operator = (String) condtions.get("conditionOp");
		if(operator != null) {
			if(operator.equals("and"))
				expression.setOperator(Operator.AND);
			else
				expression.setOperator(Operator.OR);
		}
		
		// get the no. of conditions in a rule
		    JSONArray conditionArray = (JSONArray) condtions.get("cond");
		
		// prepare java code for each condition
		for(int i = 0; i < conditionArray.length(); i++) {
			
			ObjectMapper objectMapper = new ObjectMapper();
			Condition condition = objectMapper.readValue(conditionArray.get(i).toString(), Condition.class);
			new CodeGenerator();
		
			// get java hierarchy for business field
			String codeToAccessField = CodeGenerator.getCodeForBussinessField(condition.getIfValue(), false);

			// map java hierarchy mapping to the insurance object
            FieldAccessExpr ifField = new FieldAccessExpr(insuranceClaim, codeToAccessField);
            
            // get the operator with which we want to check with the value
            MethodCallExpr conditionExpr = new MethodCallExpr(ifField, getOperatorMap().get(condition.getOperatorValue()));
            if(condition.isStatic()) {
            	if(CodeGenerator.filteredMappingSheetRow.getCell(2).toString().equalsIgnoreCase("String"))
            		conditionExpr.addArgument(new StringLiteralExpr(condition.getSetValue()));
            	else
            		conditionExpr.addArgument(new NameExpr(condition.getSetValue()));
            }
            else
            	conditionExpr.addArgument(new NameExpr(insuranceClaim + "." +CodeGenerator.getCodeForBussinessField(condition.getSetValue(), false)));
            
            if(conditionArray.length() > 1) {
            	if(i == 0) 
               	 expression.setLeft(conditionExpr); 
               else
               	 expression.setRight(conditionExpr);
            	
            	ifStmt.setCondition(expression);
            }
            else
            	ifStmt.setCondition(conditionExpr);
		}
		if(conditionArray.length() > 1)
			ifStmt.setCondition(expression);
		
		return ifStmt;
		
	}
	
	public static BlockStmt prepareThenStatementCode(JSONObject ruleObject, NameExpr insuranceClaim) throws Exception {
		JSONArray actions = (JSONArray) ruleObject.get("actions");
		BlockStmt thenBlock = new BlockStmt();
		NodeList<Statement> thenBlockStatements = new NodeList<>();
		for(int i = 0; i < actions.length(); i++) {
			JSONObject thenObject = actions.getJSONObject(i);
			String thenFieldSetterCode = CodeGenerator.getCodeForBussinessField((String)thenObject.get("setValue"), true);
			FieldAccessExpr thenField = new FieldAccessExpr(insuranceClaim, thenFieldSetterCode);
			MethodCallExpr thenSetterExpr = new MethodCallExpr();
            thenSetterExpr.setName(thenField.toString());
            boolean isStatic = (Boolean)thenObject.get("isStatic");
            if(isStatic) {
            	if(CodeGenerator.filteredMappingSheetRow.getCell(2).toString().equalsIgnoreCase("String"))
            		thenSetterExpr.addArgument(new StringLiteralExpr((String)thenObject.get("toValue")));
            	else
            		thenSetterExpr.addArgument(new NameExpr((String)thenObject.get("toValue")));
            }
            else {
            	thenSetterExpr.addArgument(new NameExpr(insuranceClaim + "." + CodeGenerator.getCodeForBussinessField((String)thenObject.get("toValue"), false)));
            }
            thenBlockStatements.add(new ExpressionStmt(thenSetterExpr));
		}
		thenBlock.setStatements(thenBlockStatements);
		return thenBlock;
	}
	
	public static Map<String, String> getOperatorMap(){
		Map<String, String> operatorMap = new HashedMap<>();
		operatorMap.put("=", "equals");
		return operatorMap;
	}
	
	public static void main(String[] args) throws Exception {
		ParseJSON parseJSON = new ParseJSON();
		parseJSON.parseJson("C:\\Users\\addade\\Desktop\\Work\\Innovation\\input.json");
	}
}
