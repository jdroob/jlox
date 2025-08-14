JAVAC := javac
JAVA := java
BIN_DIR := bin
SRC_DIR := src/com/craftinginterpreters/lox
TOOL_DIR := src/com/craftinginterpreters/tool
PKG_LOX := com.craftinginterpreters.lox.Lox
AST_PRINTER := com.craftinginterpreters.lox.AstPrinter
RPN_PRINTER := com.craftinginterpreters.lox.RpnPrinter
TREE_PRINTER := com.craftinginterpreters.lox.TreePrinter
PKG_TOOLS := com.craftinginterpreters.tool.GenerateAst
ARGS :=

.PHONY: default
default: $(BIN_DIR)
	$(JAVAC) $(SRC_DIR)/*.java -d $(BIN_DIR)
	$(JAVAC) $(TOOL_DIR)/*.java -d $(BIN_DIR)

.PHONY: run
run: default
	$(JAVA) -cp $(BIN_DIR) $(PKG_TOOLS) $(SRC_DIR)
	$(JAVA) -cp $(BIN_DIR) $(AST_PRINTER)
	$(JAVA) -cp $(BIN_DIR) $(RPN_PRINTER)
	$(JAVA) -cp $(BIN_DIR) $(TREE_PRINTER)
	$(JAVA) -cp $(BIN_DIR) $(PKG_LOX) $(ARGS)

.PHONY: clean
clean:
	rm -rf $(BIN_DIR)

$(BIN_DIR):
	mkdir -p $(BIN_DIR)
