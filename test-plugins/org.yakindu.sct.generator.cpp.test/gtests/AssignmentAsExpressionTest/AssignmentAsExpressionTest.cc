/**
* Copyright (c) 2017 committers of YAKINDU and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     committers of YAKINDU - initial API and implementation
*/
#include <string>
#include "gtest/gtest.h"
#include "AssignmentAsExpression.h"
#include "sc_types.h"
static AssignmentAsExpression* statechart;

class StatemachineTest : public ::testing::Test{
	protected:
	virtual void SetUp() {
		statechart = new AssignmentAsExpression();
		statechart->init();
	}
	virtual void TearDown() {
		delete statechart;
	}
};


TEST_F(StatemachineTest, simpleAssignment) {
	
	
	statechart->enter();
	
	EXPECT_TRUE(statechart->isStateActive(AssignmentAsExpression::main_region_Add));
	
	EXPECT_TRUE(statechart->getDefaultSCI()->get_b()== 5l);
	
	EXPECT_TRUE(statechart->getDefaultSCI()->get_a()== 9l);
	
	statechart->runCycle();;
	
	EXPECT_TRUE(statechart->isStateActive(AssignmentAsExpression::main_region_Subtract));
	
	EXPECT_TRUE(statechart->getDefaultSCI()->get_d()== 6l);
	
	statechart->runCycle();;
	
	EXPECT_TRUE(statechart->isStateActive(AssignmentAsExpression::main_region_Multiply));
	
	EXPECT_TRUE(statechart->getDefaultSCI()->get_e()== 15l);
	
	statechart->runCycle();;
	
	EXPECT_TRUE(statechart->isStateActive(AssignmentAsExpression::main_region_Divide));
	
	EXPECT_TRUE(statechart->getDefaultSCI()->get_g()== 1l);
	
	statechart->runCycle();;
	
	EXPECT_TRUE(statechart->isStateActive(AssignmentAsExpression::main_region_Modulo));
	
	EXPECT_TRUE(statechart->getDefaultSCI()->get_i()== 1l);
	
	statechart->runCycle();;
	
	EXPECT_TRUE(statechart->isStateActive(AssignmentAsExpression::main_region_Shift));
	
	EXPECT_TRUE(statechart->getDefaultSCI()->get_j()== 16l);
	
	EXPECT_TRUE(statechart->getDefaultSCI()->get_k()== 4l);
	
	statechart->runCycle();;
	
	EXPECT_TRUE(statechart->isStateActive(AssignmentAsExpression::main_region_boolean_And));
	
	EXPECT_TRUE(statechart->getDefaultSCI()->get_l()== 1l);
	
	statechart->runCycle();;
	
	EXPECT_TRUE(statechart->isStateActive(AssignmentAsExpression::main_region_boolean_Or));
	
	EXPECT_TRUE(statechart->getDefaultSCI()->get_p()== 15l);
	
	statechart->runCycle();;
	
	EXPECT_TRUE(statechart->isStateActive(AssignmentAsExpression::main_region_boolean_Xor));
	
	EXPECT_TRUE(statechart->getDefaultSCI()->get_u()== 12l);
	
	statechart->exit();
	
}
