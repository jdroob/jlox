## The plan
- Learn some background on language implementations.
- Write a tree-walk interpreter in Java.
   - In parallel, write the same tree-walk interpreter in C.
- Write a bytecode interpreter in C.

## Language Implementations

### Definitions

- **Interpreter**: A program that takes source code as input and, in a single process, executes the program.  
   - Interpreters _can_ contain compilers (e.g., compile source code to bytecode and then execute the bytecode).  
   - Interpreters execute bytecode.  
   - Interpreters are generally implemented in two ways: tree-walk interpreters and bytecode interpreters.  

- **Tree-walk interpreters**: Scan and parse the source code, creating an Abstract Syntax Tree (AST). The interpreter then walks the AST, executing the program as it traverses the tree.

- **Bytecode interpreters**: Compile the source code to bytecode, an intermediate representation between source code and machine code. Bytecode is executed by a _virtual machine_ (VM), such as the Java Virtual Machine (JVM) or the Python interpreter.

- **Compiler**: A program that transforms source code into a different, semantically equivalent representation. Examples include:  
   - Machine code (e.g., GCC, Clang).  
   - Bytecode (e.g., CPython, `javac`).  
   - Source code of other languages (e.g., TypeScript compiler compiles TypeScript to JavaScript).

### Ch 3 Exercises

**1. Explore the lexer and scanner of a real language implementation**  
- I explored the lexer and scanner in `jq`.

**2. JITs are the fastest way to implement dynamically typed languages. Why don't all dynamically typed languages do this?**  
- It's harder to implement.  
- It ties the implementation to a specific architecture (though interpreters do as well).  
- JITs have a _startup cost_ (e.g., identifying hot spots). For languages often used for short programs, the startup cost outweighs the benefits.

**3. Most LISP implementations that compile to C often include an interpreter to execute LISP code on the fly. Why?**  
- Lisp uses a macro system. Statements containing macros either need to be preprocessed and compiled to C _or_ interpreted directly. Preprocessing and compiling incur a greater performance hit than interpreting these statements.

### Ch 4 Exercises

**1. Python and Haskell are not _regular_. What does that mean and why aren't they?**  
- A _regular_ language (in formal language theory) can be expressed as a regular expression. Most programming languages are not regular because balancing parentheses cannot be expressed with regular expressions.  
- Python's syntax is not a context-free grammar (CFG) because CFGs cannot capture its INDENT-DEDENT syntax.

**2. Spaces are usually not significant. In the C preprocessor, they are. Explain how.**  
- `#define MACRO(P) (P + 1)` defines a macro that takes input `P` and returns `P + 1`.  
- `#define MACRO (P) (P + 1)` substitutes `MACRO` with `(P) (P + 1)`.

**3. When might you want to hold onto comments?**  
- To support features like docstrings.

### Ch 5: Introduction to Grammars

Grammars are formal systems used to define the syntax of languages. They describe the structure of valid strings in a language.

#### Supporting Recursion

Grammars can support recursion, enabling the definition of complex, nested structures. One specific type of recursion is **coordinated recursion**, where recursive elements follow specific patterns.

For example:

```
breakfast -> [main] with [side]
main -> eggs | eggs with [side]
side -> [main] | toast

Examples:
- eggs with toast
- eggs with eggs with toast
- (eggs with (eggs with toast) with toast) with toast
```

In this example:  
- The `main` rule is recursive, allowing repeated nesting.  
- The `with` portion ensures proper coordination between `main` and `side`.

This coordination is what context-free grammars provide, which regular languages cannot express.

#### The Expression Problem

The *Expression Problem* explores the trade-offs between extending a program by adding new types or new operations.

**Object-Oriented Approach:**

```java
abstract class Shape {
    abstract double area();
}

class Square extends Shape {
    double side;

    Square(double side) {
         this.side = side;
    }

    @Override
    double area() {
         return side * side;
    }
}

class Circle extends Shape {
    double radius;

    Circle(double radius) {
         this.radius = radius;
    }

    @Override
    double area() {
         return Math.PI * radius * radius;
    }
}
```

**Functional Approach:**

```scala
sealed trait Shape
case class Circle(radius: Double) extends Shape
case class Square(side: Double) extends Shape

def area(shape: Shape): Double = shape match {
    case Circle(radius) => Math.PI * radius * radius
    case Square(side) => side * side
}
```

**Adding a New Type:**  
- Object-Oriented: Define a new class.  
- Functional: Update the type definition and the function handling it.

**Adding a New Operation:**  
- Object-Oriented: Update every class to include the new method.  
- Functional: Define a new function.

### Summary

- **Object-Oriented Approach:** Easier to add new types, harder to add new operations.  
- **Functional Approach:** Easier to add new operations, harder to add new types.

#### The Visitor Pattern

This is all to motivate the introduction of The Visitor Pattern. The Visitor Pattern addresses the problem of adding new operations to an object-oriented language without modifying existing classes. This is particularly useful when implementing parsers, where new operations (e.g., `interpret()`) are frequently added to expression types like `Binary`, `Grouping`, `Unary`, and `Literal`.

What is the main goal of the visitor pattern? To make it easier to add operations to an object-oriented program without having to refactor everything (like the expression problem describes). In essence, we want to _approximate_ the ease of adding an operation to a program written in a functional language, but in an OOP language. We're going to define the behavior for a new operation for a **set of types** in one place.



## Ch 10: Functions
- A function definition is REALLY just a binding of a _function object_ to a name
- A function object is just a block of code that returns a value when called (if no return statement, nil is returned). In other words, a function object is just another kind of expression :)
- Functions also have the special property of being _callable_ (so do other objects as we'll see later)
- RANDOM: the comma rule in the parser can cause ambiguity with function call arg list
           - solution? each arg should be eval'd at assign() level and no lower :)

- Challenge 2 of chapter 10 was to implement anonymous functions. Here's what I did:
    - Background: "regular" function definitions are statements
    - These statements are interpreted by binding the function object to a name
        e.g. fun sayHi() { print "hi"; } is syntactic sugar for var sayHi = fun () { print "hi"; }
    - I decided to treat an anonymous function as an expression.. since it is I think?
    - It's really just like... a thing, ya know?
        e.g. 4; and fun () { print "hi"; }; are just two expression statements that produce a value and don't really do anything.
        In the former case, the value is 4. In the latter case, the value is the function object
    - So I just added a condition in the parser to lookahead 2 tokens to check if it's a "fun" then IDENTIFIER or not.
    - If not, then it's an anonymous function.
    - Anonymous functions are interpreted by simply returning a new LoxFunction object.

- Challenge 3:
    Is this program valid?
    ```
    fun scope(a) {
        var a = "local";
    }
    ```
    In other words, are a function’s parameters in the same scope as its local variables, or in an outer scope? What does Lox do? What about other languages you are familiar with? What do you think a language should do?


    Answer: In C, this is NOT valid

    ```
        $ cc scopeTesting.c -Wall -o scopeTesting
        scopeTesting.c: In function ‘func’:
        scopeTesting.c:5:13: error: ‘a’ redeclared as different kind of symbol
            5 |         int a = 99;
            |             ^
        scopeTesting.c:4:15: note: previous definition of ‘a’ with type ‘int’
            4 | void func(int a) {
            |           ~~~~^
        $ cat scopeTesting.c
        #include <stdlib.h>
        #include <stdio.h>

        void func(int a) {
                int a = 99;
                printf("a: %d\n", a);
        }

        int main(void) {
                func(42);
                return 0;
        }
    ```
    This is because in C, parameters exist in the same scope as local function variables.
    Lox is the same - but Lox allows for "re-declaration / definitions".
    The param 'a' exists in the same environment as the var a, var a is just allowed to override the parameter.
    For a scripting language, this is fine. What are the alternative options? We could shadow the function param.
    But that'd be pointless (it'd never get used). The only other option is to throw an error which seems a bit 
    harsh for the vibe we're going for with Lox.

- Challenge 1: Our interpreter carefully checks that the number of arguments passed to a function matches the number of parameters it expects. Since this check is done at runtime on every call, it has a performance cost. Smalltalk implementations don’t have that problem. Why not?

My Answer: I'm assuming SmallTalk statically checks each call for the correct number of args. <- Fool! This ain't no statically typed language :)

Real Answer: SmallTalk uses a "message passing" style of calling functions. For example, to insert the string "john" at index 2 of a list in Python, the syntax is:

```python
>>> lis = [None] * 4
>>> lis
[None, None, None, None]
>>> lis.insert("john", 2)
>>> lis.insert(2, "john")
>>> lis
[None, None, 'john', None, None]
```
In SmallTalk, this looks like:

```smalltalk
lis insert: "john" at: 2
```

The combination of the keyword 'insert', the colon, the keyword 'at', the second colon constructs a method name, "insert:at:"
which is used as a lookup key to the appropriate function object. Notice that the 2 colons implicitly encode the expected number of parameters. Importantly, the only way to call this method is by writing a statement with the syntax: IDENTIFIER "insert" ":" primary "at" ":" NUMBER. Anything that differs from this (e.g. something with the wrong number of arguments) either wouldn't parse or would crash since the lookup would fail.


## Ch 11: Resolving and Binding

# Intro
- In the previous chapter, we introduced _closures_ a data structure that bundles a function object with an environment.
- Apparently, this caused an inconsistency in our scoping rules that must be rectified...

# 11.1 Static Scope
- As we're aware, Lox uses **lexical scoping** - meaning that you can determine which _declaration_ a variable _use_ refers to by inspecting the program text.

For example:

```C
var a = 42;

{
    var a = 43;
    print a;    // 43
}
```

- We can look at this and determine that the 'a' being printed is associated with the declaration: `var a = 43;`. In other words, lexical scoping implies that a variable's value is determined by the value assigned in the "top-most" (or "bottom-most" depending how you think about which way the stack of enviornments is growing) environment.

- This actually wasn't too far off from the book's definition :)

```
A variable usage refers to the preceding declaration with the same name in the innermost scope that encloses the expression where the variable is used.
```

- Breaking this down..
    - The terminology "variable usage" is intentional because it's meant to refer to both variable assignments (e.g. a = 99;) and variable expressins (e.g. print a;).
    - _preceding_ simply meaning before in the program text 
    - _innermost_ is there to help disambiguate which usage a variable refers to. There could be multiple declarations of a variable in different scopes (shadowing) prior to the usage of a. The _innermost_ rule indicates that the declaration in the innermost scope **that also encloses the expression where the variable is used** is where the declaration associated with the variable usage can be found.

- With this in mind, we can now introduce the edge case we're failing

```C
var a = "global";
{
    fun showA() {
        print a;
    }

    showA(); // global
    var a = "block";
    showA(); // block
}
```

- Okay... based on our rules, the 'a' in `print a` should be assigned "global". The reason being that the defintion of the function `showA` occurs in a block scope after the definition of the global variable 'a'. **Another important thing to keep in mind** is that the value of a variable used in a closure **should not change unless it's explicitly redefined in the function object**. Since 'a' is not redefined in the function object, `showA()` should always print "global". Unfortunately, the interpreter in its current state will print "global" followed by "block". This is because the environment associated with closures are currently _mutable_. Once the environment assoc'd with a closure is changed, the function object of the closure will reflect these changes. This is **not** what we want (because we want to preserve the 'snapshot' nature of closures).


- **SUBTLE POINT**: Notice that the environment component of the closure: `showA` looks like: {'a' : "global"} <-- {'showA' : <fn object>} right after the definition of `showA` (before the first call).
- When `showA` is FIRST called, the body of `showA` is executed and the closure's environment has not changed. Therefore, the correct `a` is referenced.
- Next, we declare a new `a` in the block scope. Now the environment looks like {'a' : "global"} <-- {'showA' : <fn object>, 'a' : "block"}.
- THIS is a problem because the closure: `showA`'s environment has now been mutated.
    * The mutation itself isn't _really_ the problem (Environments will always be mutated if you want programs that do stuf) - it's how we're currently looking up variable declarations. This program illustrates a fatal error in the "just walk environments backward" approach.
- What we need is to know the EXACT location of the Environment containing the correct variable declaration. That's the point of the Resolver pass.


## 11.2
- TODO: Add real notes here
- essentially, we're trying to write a pass _after_ parsing and _before_ interpretation to allow variable usages to be resolved (i) correctly and (ii) in linear time. The core idea here is: for each variable usage, there exists a unique variable declaration that it should refer to (see the rule above). This variable declaration exists in some scope. We want to write a pass to know exactly **how many hops back from the variable usage's scope to the correct declaration's scope**. We will accomplish this through a resolution (semantic) analysis pass.

- TODO: Add 11.3 - 11.5 notes
- **Why shouldn't we allow function defs after calls like in statically-typed, compiled languages?**
    - To illustrate why this is difficult, consider how we'd try doing this:
        - In the resolving pass, we'd pass the function name and definition to the interpreter to add to some side-table. But wait? In the variable side-table, we're using the depth to traverse the **dynamically-generated** linked list of Environments from inner to outer.
        - We cannot traverse furthre inward to a function definition that doesn't exist yet.
        - We'd need some way to have access to the function definition at call time.
        - What if we just define the function definition globally?
        - This would work if we didn't care about closures - which we do in Lox.
        - So in Lox, and reasonable dynamically generated languages, lexically defining functions after the call is generally something that doesn't happen.

    - Random bug while implementing Resolver:
        - I was running into an issue where scripts like:
        ```python
            fun func(a) {
                print a;
            }

            func("hi");
        ```
        were failing due to 'Undefined variable \'a\'.'
        - I suspected this had to do with the resolver... and it did!
        - In resolveFunctionDef and resolveAnonExpr, I was beginning a
         scope for the function via beginScope() and beginning a scope
         for the block in visitBlockStmt() via beginScope(). This was messing up the hop value.
         - I ended up resolving all block statement statements from the function visitor instead.
## Challenges
- 1) Q: Why is it safe to eagerly define the variable bound to a function’s
        name when other variables must wait until after they are initialized
        before they can be used?
     A: I think a good way to illustrate why this case is safe is to discuss the unsafe case:
        eagerly defining a variable. We've made the design decision to not allow cases like:
        ```python
        var a = "global";
        {
            var a = a;
        }
        ```
        To enforce this decision, the resolver does NOT eagerly define var statements. Rather, the LHS 'a'
        is declared. The RHS initializer is resolved and checked for a matching var name. If a matching var name
        exists (like it does here), an error is raised.

        The difference with function definitions is that the object being assigned cannot be a variable. Otherwise, it wouldn't be a function definition. So we know we're always avoiding the unsafe case,
        so we can eagerly define functions.

- 2) 
    Q: How do other languages you know handle local variables that refer to the same name in their initializer, like:
     ```python
     var a = "outer";
     {
        var a = a;
     }
     ```
     Is it a runtime error? Compile error? Allowed? Do they treat global
     variables differently? Do you agree with their choices? Justify your
     answer

     A: In C, the inner 'a', is defined first. Then it's RHS is evaluated. In this case,
        the 'a' in the inner var declaration's RHS is the inner 'a' which is uninitialized.
        So we're initializing inner 'a' with itself. So it isn't being initialized even though it is...

        I think this is error-prone. If we want to allow this type of behavior, to me the logical explanation
        for what the user is trying to do is initialize inner 'a' with global 'a'. Therefore, I believe the RHS initializer should be evaluated before inner 'a' is defined.

- 3) Q:  Extend the resolver to report an error if a local variable is never used.
     A: Donezo - added an `isUsed` field to ResolverInfo class and checked for any false && VARIABLE in endScope()

- 4) Q: Our resolver calculates which environment the variable is found in, but
     it’s still looked up by name in that map. A more efficient environment
     representation would store local variables in an array and look them up
     by index.
     Extend the resolver to associate a unique index for each local variable
     declared in a scope. When resolving a variable access, look up both the
     scope the variable is in and its index and store that. In the interpreter,
     use that to quickly access a variable by its index instead of using a
     map.

     A: Yikes - that sounds like a lot of refactoring... I'll try that in a separate branch and get back to you.

## Ch 12: Classes

Dev log:
    Random things i learned from implementing classes in Lox.

    Parsing trick: converting a Expr.Get node to Expr.Set node when parsing an assignment to a field expression like a.b = c

    Instances are basically field look up tables with a pointer to the class they instantiate

    Classes (static class members arent implemented yet) are basically method look up tables.

    At runtime, a class definition statement binds the class name to a class object in the dynamic environment.

    When a class object is instantiated, the class object is called. The call performs 2 actions - creating a new instance and running the constructor (if defined). The instance is then returned which could be considered a third operation I suppose.

    'this' refers to the instance that invoked the method being executed

    The keyword 'this' is bound to a method as follows: an Expr.Get expression is evaluated. The instance's field LUT is checked. Nothing. Next, the class's method LUT is checked and theres a hit. The function object is returned. The bind() method is called on that function object which creates a new inner environment containing 'this' (associated with the calling LoxInstance). This info is used to initialize the LoxFunction object which is returned. From there, the call expression is evaluated and the class method is executed (with 'this' pointing to the correct object)


    --
    That's a great and insightful summary! You're clearly internalizing how object-oriented features map onto a simple interpreter. Here’s a breakdown and a few reflections or expansions on what you wrote, in case you find them helpful or want to dive deeper:


    ---

    ✅ Parsing trick: Expr.Get → Expr.Set

    > "Converting a Expr.Get node to Expr.Set node when parsing an assignment to a field expression like a.b = c."



    This is a really elegant parsing trick. Instead of introducing a whole new syntax rule, you reuse the existing expression structure:

    a.b = c

    This is parsed as:

    Parse a.b as a Get expression,

    See the = token,

    Wrap it in a Set expression: Set(object=a, name="b", value=c).


    This sort of retrospective parsing (looking back and changing the tree node) is a practical technique in recursive descent parsers.


    ---

    ✅ Instances as field LUTs with a class pointer

    > "Instances are basically field lookup tables with a pointer to the class they instantiate."



    Yes — it's the same design used in many dynamic languages (e.g. Python's __dict__ and __class__). This design:

    Keeps instance data (fields) separate from shared methods.

    Makes method lookup and field access clear and modular.

    Makes instance state mutable while class definitions stay static.



    ---

    ✅ Classes as method LUTs

    > "Classes (static class members aren’t implemented yet) are basically method look up tables."



    Exactly. A class in this context is just a named map of method names to function bodies, optionally with a superclass pointer if inheritance is implemented. Adding static members later would just mean:

    Having a second lookup table for static members (or methods),

    Or treating the class itself as an instance of a LoxMetaClass.



    ---

    ✅ Class statement binds class object to class name in environment

    > "At runtime, a class definition statement binds the class name to a class object in the dynamic environment."



    This follows the idea of "classes as first-class values". You can pass them around, assign them to variables, etc. It’s another way Lox (like Python, Lua, etc.) blurs the line between functions, classes, and other values.


    ---

    ✅ Class instantiation = create instance + run constructor

    > "When a class object is instantiated, the class object is called. The call performs 2 actions - creating a new instance and running the constructor (if defined)."



    Perfect! Two-phase instantiation:

    1. Allocate a new LoxInstance (the field table).


    2. If there's an init() method, bind it and call it with the instance as this.



    This two-step process lets you cleanly separate memory allocation from initialization, just like in many real languages (C++, Java, etc.).


    ---

    ✅ How this is bound during method calls

    > "The keyword 'this' is bound to a method as follows..."



    You captured the chain of reasoning well:

    Evaluate a.b, which is a Get on object a.

    If not a field, check the class for a method.

    If found, bind the method to the instance.

    The binding wraps the function in a new environment with this = a.

    The method call uses this environment.


    This delayed binding model is powerful. In Lox, methods are "free functions" until they're accessed — at which point they're curried with a this binding, transforming them into bound methods.


    ---

    💡 Bonus Thought: "Method Binding is Late"

    In statically-typed OOP languages, method resolution is mostly determined at compile-time. In Lox, everything is late-bound at runtime. You see that clearly in:

    someInstance.method()

    At runtime:

    You look up method dynamically,

    Then wrap it dynamically with this,

    Then call it dynamically.


    It’s flexible but also slower — good for exploring dynamic dispatch systems.


    ---

    If you're planning to extend the system (e.g., inheritance, super calls, static methods), this foundation will pay off nicely. Would you like a suggestion for a next step to explore (like inheritance or metaclasses)?
    - 7/25/25: forced `init()` methods to always return `this`
    - 7/25/25: thow error if `init()` returns value from `init()` (plain old `return;` is fine)
    - 7/25/25: fixed bug where 'Cannot return from top-level' error was being thrown when anonymous functions contained return statements. Fixed by adding ANONYMOUS option to FunctionType.


    - 7/25/25: just an observation: notice the 'wrapper' environments for 'this' before methods are exec'd
        - Consider the following:
        ```python

        // Example.lox
        class Example {
            init(data) {
                this.data = data;
            }

            get() {
                return this.data;
            }
        }

        fun main() {
            var e = Example("john");
            print e.get();  // "john"
        }

        main();
        ```
        - The order of events are:
            - interpreter executes Stmt.ClassDef statement
                - In dynamic env, 'Example' maps to class object
                - class object contains init() and get() methods
                - When interpreting the init() method declation statement,
                  the method is marked as an initializer
                    - This causes this LoxFunction object to be treated differently than other LoxFunction objects (that are also class methods)
                    - Specifically, the init LoxFuntion returns a reference to itself rather than null when it's called
                    - During the resolver pass, if the resolver detects a value being returned from init() an error is thrown - in Lox, init() always returns a reference to itself. Other values cannot be returned from init().
                - get() is added to Example's method LUT like any other method
                - main() is added to the dynamic environment - mapping the name 'main' to its function object.
                - main() is called - the function object is retrieved. The function object is called. The statements within main's body are executed.
                - In the var statement:
                    - the name 'e' is added to the dynamic environment
                    - The rhs is evaluated
                        - Example class is called
                        - An Example instance is created
                        - The instance's init method is looked up
                        - A new LoxFunction is created with 'this' bound to it
                            - A new, inner, environment is created (with the global environment as its enclosing env)
                            - The keyword, 'this' is bound to the new instance in this new inner env.
                            - This environment + the funcDef object that was looked up are used to create a new LoxFunction object that is then called. This way, any fields created during init() are updated in the instance's field LUT.

                        ENV SNAPSHOT:
                        ----------------
                        global: ['Example' : <class-object>, 'main': <func-object>]
                        
                        ^
                        |
                        
                        inner0: (main function) []

                        ^
                        |

                        inner1: ['this': <Example-instance-object_0>]

                        ^
                        |

                        inner2 (init method): ['data': "john"]
                        -------------------
                        
                        - The LoxInstance object is returned and bound to the name 'e'.
                    
                        ENV SNAPSHOT:
                        ----------------
                        global: ['Example' : <class-object>, 'main': <func-object>]
                        
                        ^
                        |

                        inner0 (main function): ['e': <Example-instance-object_0>]
                        -------------------

                    - Next, the interpreter executes the print statement.
                        - e.get() is evaluated
                            - 'e' is looked up in the dynamic environment and <Example-instance-object_0> is returned
                            - Within the instance, the property 'get' is looked up. First in the Instance's field LUT (nothing found). Then in the Example class's method LUT ('get' LoxFunction found and returned).
                            - The instance recognizes that the property is a method, so it binds the keyword 'this' to itself
                        
                        ENV SNAPSHOT:
                        ----------------
                        global: ['Example' : <class-object>, 'main': <func-object>]
                        
                        ^
                        |

                        inner0 (main function): ['e': <Example-instance-object_0>]

                        ^
                        |

                        inner1 : ['this' : <Example-instance-object_0>]
                        -------------------

                            - From there, the 'get' LoxFunction object is called and is executed.

                        ENV SNAPSHOT:
                        ----------------
                        global: ['Example' : <class-object>, 'main': <func-object>]
                        
                        ^
                        |

                        inner0 (main function): ['e': <Example-instance-object_0>]

                        ^
                        |

                        inner1 : ['this' : <Example-instance-object_0>]

                        ^
                        |

                        inner2 (get method) : []
                        -------------------

                            - The print statement is executed
                            - The main function has now finished executing
                            - The program has now finished executing

7/25/25:

    I was thinking it's kind of weird that we don't track environments when declaring classes when I realized we do that through class methods. When interpreting a class declaration statement, we're adding methods to a method list and one of the fields in a LoxFunction is a closure which points to the enclosong environment.

    Maybe the reason i was confused is because i was thinking of var declaration statements in other languages. In Lox, those statements would indicate static members. That is - members that belong to the class, not each individual instance (although these members can be accessed via the class or instances). In this case though, rather than store these in the dynamic environment, these should have their own dedicated static member LUT in the class. Same for static methods.


    ----

    You're absolutely right, and you're making excellent observations about how environments, closures, and class semantics all interact in Lox. Let’s unpack and affirm your ideas a bit:


    ---

    ✅ 1. Closure Tracking via Method Environment

    You correctly noted that Lox doesn’t explicitly store an environment on the class, but each method is wrapped in a LoxFunction, which carries the closure of the environment where it was declared. This is how:

    Methods can reference variables from the surrounding scope at the time the class was defined.

    For example, a class body like this:

    var a = "outer";

    class Foo {
    method() {
        print a;
    }
    }

    ...will work because Foo.method closes over the environment where a was declared.



    ---

    ✅ 2. No Static Members Yet — But Great Intuition

    Your insight about var statements inside the class not being treated as static members is spot on.

    In Lox (as implemented in Crafting Interpreters), there is no syntactic support for static members or methods.

    All fields belong to instances, and the class object itself just holds a method table (plus super, if applicable).


    But you're right to say:

    If we were to add static members, they shouldn't go into the interpreter’s dynamic environment (like Environment.define(...)) because they don’t live in the scope surrounding the class — they live on the class object itself.

    So you'd instead extend the LoxClass with a new map:

    final Map<String, Object> staticFields;

    And provide logic like:

    Foo.bar looks in the class’s static field map.

    Foo.baz() calls a LoxFunction where isStatic == true, and optionally bind(null) instead of bind(instance).




    ---

    ✅ 3. Static Method Resolution

    You'd also want to distinguish between instance methods and static methods in your parser and class representation:

    When declaring a static method (e.g., static greet() { ... }), you'd store it in a separate Map<String, LoxFunction> inside LoxClass.

    When evaluating a call to Foo.greet(), your interpreter looks in that static method map.



    ---

    🧠 Summary

    You're not "missing" environment tracking — method closures are the way environment context is preserved.

    Static members are conceptually different: they belong to the class object, not each instance, and don't go in the dynamic scope.

    You’d extend LoxClass to support static fields and methods by adding dedicated maps.


    Would you like help sketching out how to add static methods and fields to your Lox interpreter?                            

    7/27/2025:
        - moving to Sac... phew!
        - implemented static methods for classes (first attempt)
        - Added staticMethods map to LoxClass
        - Added staticMethods lookup method (and call in LoxInstance.get())
        - Added LoxClass.get() method
        - Added LoxFunction.bind(LoxClass) method
        - Added check in resolver to ensure that init is not static

    7/28/2025:
        - very cool way of implementing static methods AND static (class) fields for the price of one
        - branched off yesterday's static methods implementation
        - got rid of LoxClass.get
        - Had LoxClass extend LoxInstance
            - feels weird for class to implement instance, right?
            - Well, the reason we're doing this is b/c it allows us to 
            inherit LoxInstance's get() and set() methods AND (importantly) LoxInstance's fields LUT
            - Basically, we get to treat Lox classes in a similar way to the way we treat Lox instances
                * is that what a metaclass is?

        - From there, we do something key:
            - We modify LoxInstance.get to check:
                1) its own fields LUT
                2) its klass fields LUT (only if its a LoxInstance)
                    * since LoxClass extends LoxInstance, LoxClass now has a 'fields' LUT
                3) its methods LUT
                    * throws error if we determine a LoxClass is trying to access a non-static method
                    *uses 'klass' if not a LoxClass; 'this' otherwise
                4) its staticMethods LUT
                    * uses 'klass' if not a LoxClass; 'this' otherwise

        - The fallout from the above bullet is that the class object will have its own fields LUT for static data that can be shared across instances
        
        - Furthermore, class objects cannot access non-static methods
            *otherwise what's the point of differentiating b/w static and non-static?

    7/29/2025:
        - added support for getterMethods
            - Added parsing logic
                - just make param lists optional
            - Added isGetterMethod to Stmt.FunctionDef
            - In visitClassStmt - if (funcDef.isGetterMethod) # create LoxFunction with isGetter = true;
            - Added isGetter field to LoxFunction
            - In visitGetExpr - # if value returned is LoxFunction and isGetter == true, call function
        - Fixed 'stuck on currentFunction = FunctionType.INITIALIZER' error
            - In resolver, in class method resolveFunctionDef loop, if (isInit) declaration = INITIALIZER # forgot to reset to METHOD as default

    7/30/2025:
        - finally added a .gitignore
        - was getting below error
            ```log
            Can only call functions or classes.
            [line 1]
            ```
            - Eventually figured out it was b/c I was switching the isGetter and isStatic params and was trying to call a getter method (i.e. I was trying to call the thing returned from a getter)
        - this leads me to my next point: I simplified the static methods implementation to just have classes use a single methods LUT and added an `isStatic` attribute to the LoxFunction class

## Challenges
    1) Implement static methods - done
        - See changes in LoxInstance.java, LoxClass.java, Parser.java, Resolver.java (I think that's it?)
        - See dev notes above for implementation details

    2) Implement getter methods - done
        - Same deal as (1)

    3) Python and JavaScript allow you to freely access an object’s fields
    from outside of its own methods. Ruby and Smalltalk encapsulate
    instance state. Only methods on the class can access the raw fields, and
    it is up to the class to decide which state is exposed. Most statically
    typed languages offer modifiers like private and public to control
    which parts of a class are externally accessible on a per-member basis.
    What are the trade-offs between these approaches and why might a
    language prefer one or the other? 

    Guess:
        ```
        Python and JavaScript allow you to freely access an object’s fields
    from outside of its own methods.
    ```

        Pros: ease of use - don't need to think as carefully about designing class and its methods as sole mechanism for updating an object's fields. Can play a little loose and define free functions that modify fields

        Cons: Messiness. Footguns. Violating data hiding principles of OOP. While this makes life easier, this makes it difficult for developer A to design and implement a class with specific usage patterns in mind and be sure that the users of this class will follow these usage patterns. Particularly if a user, unfamiliar with the design principles, discovers s/he can directly access field F and modify this field. Suppose this modifications solves all their problems? Great! Except... what if there are "hard-to-trace" side-effects from this change. This hypothetical illustrates why public access for all may not always be a good thing.


        ```
        Ruby and Smalltalk encapsulate
        instance state. Only methods on the class can access the raw fields, and
        it is up to the class to decide which state is exposed.
        ```

        Pros: Fewer footguns. If only methods belonging to class A can modify fields belonging to class A, then code outside of the class cannot be responsible for modifying fields belonging to class A directly.

        Cons: This could make life a little cumbersome somtimes. What if we just want to modify some entity's name. Can't we just have a public name and access it directly rather than going though the whole get / set dance?

        ```
        Most statically
        typed languages offer modifiers like private and public to control
        which parts of a class are externally accessible on a per-member basis.
        ```

        Pros: Seems like a good compromise between the 'all-public' Python philosophy and the 'all-private' Ruby philosophy. Now, members that make sense to be public can be. Subclasses can access members of a superclass if the member is protected while still hiding these members from external code. Private is still an option for data that should be under lock and key.

        Cons: We've increased the breadth of the language. But we've also decreased the ease of the language and increased the complexity. Don't get me wrong, member access specifiers do increase the power of the language overall, but it does make life slightly more complicated as a user of the languge.


    7/31/2025:
        - Added support for inheritance
            - Added 'extends' IDENTIFIER parsing support
            - Added 'superClass' field to LoxClass
            - Added 'if superClass != null: # superClass.findMethod(name)' to LoxClass.findMethod()
            - Added 'if superClass != null: # return this.superClass.getField(name)' to LoxInstance.getField(name)
            - Modified LoxFunction.bind() calls in LoxInstance.getMethod() + LoxClass.call() - now bind() takes a superClass parameter and if it's not null, a superClass instance is created and the name "super" is bound to it.
                - In other words, we now bind both "this" and "super" to class methods if the instance is of a subclass


    8/1/2025:
        - Reworked how 'super' works completely
            #TODO: explain what you changed here

            NOTE: Was confused about the following:
                - When we Interpreter.visitSuperExpr, we grab a reference to 'this' and bind it to the super method we just looked up.
                - For a while this confused me:
                    - if we're calling a method and we've presumably already bound 'this' to the method being called, why do we need to do it again?
                    - MISCONCEPTION ALERT
                        - See the example

                        ```python
                        class Base {
                            method() {
                                print "base";
                            }
                        }

                        class Derived extends Base {
                            method() {
                                super.method();
                                print "derived";
                            }
                        }

                        var d = Derived();
                        d.method();
                        ```
                    - When `d.method()` is called, 'this' -> d outside of the call to Derived.method
                        * i.e. Derived.method is bound
                    - When `super.method()` is called, we need to ensure that 'this' points to 'd' and is bound to the Base.method call
                        * i.e. visitSuperExpr needs to repeat the binding process 

### Challenges

1) Lox supports only single inheritance—a class may have a single
superclass and that’s the only way to reuse methods across classes.
Other languages have explored a variety of ways to more freely reuse
and share capabilities across classes: mixins, traits, multiple
inheritance, virtual inheritance, extension methods, etc.
If you were to add some feature along these lines to Lox, which would
you pick and why? If you’re feeling courageous (and you should be at
this point), go ahead and add it.

My answer: I know multiple inheritance can be kind of a foot gun and there's the whole diamond problem thing to consider, but I went ahead and implemented multiple inheritance for fun. See below.

    8/5/2025
        - Implemented multiple inheritance
        - Implemented ability for 'super' to access any method or field in any of its parent classes (book did methods only)

        ```python
        class A {}
        A.name = "A";
        class B extends A { 
            name { 
                return super.name
            } 
        }

        var b = B();
        print b.name;   // "name"
        ```

        - ^This works
        - There was a hiccup along the way worth mentioning:
            - In the following example:

            ```log
            > class A { a() { print "a"; } }
            > class B { b() { print "b"; } }
            > A.name = B.name = "name";
            > class C extends A, B { a() { super.a(); print "in c"; } b() { super.b(); print "in c"; } }
            > var c = C();
            > c.b();
            ```

        - I was hitting an 'undefined property' error
        - This was because C inherits from A and B
        - So in `get()`, we iterate through the superClasses until we find a matching method or we return `null`.
        - For `A`, we called `get` and there was no matching method or field
        - The default behavior was for `get` to throw an error (which it was)
        - Now, if `get` is being called with `isSuper` set to `true`, no error is thrown when there isn't a match.

        - Here's the offending Java code

        ```Java
        public Object get(Token name, Boolean isSuper) {
            Object field = getField(name);
            if (field != null) return field;

            Object method = getMethod(name, isSuper);
            if (method != null) return method;

            // pre-mature error throw if isSuper is true
            throw new RuntimeError(name,
                "Undefined property " + name.lexeme + ".");
        }
        ```

        - And here's the corrected version:

        ```Java
        public Object get(Token name, Boolean isSuper) {
            Object field = getField(name);
            if (field != null) return field;

            Object method = getMethod(name, isSuper);
            if (method != null) return method;

            if (!isSuper) {
                throw new RuntimeError(name,
                    "Undefined property " + name.lexeme + ".");
            }

            return null;
        }
        ```
2) In Lox, as in most other object-oriented languages, when looking up a
method, we start at the bottom of the class hierarchy and work our way
up—a subclass’s method is preferred over a superclass’s. In order to
get to the superclass method from within an overriding method, you
use super.
The language BETA takes the opposite approach. When you call a
method, it starts at the top of the class hierarchy and works down. A
superclass method wins over a subclass method. In order to get to the
subclass method, the superclass method can call inner, which is sort
of like the inverse of super. It chains to the next method down the
hierarchy.
The superclass method controls when and where the subclass is
allowed to refine its behavior. If the superclass method doesn’t call
inner at all, then the subclass has no way of overriding or modifying
the superclass’s behavior.
Take out Lox’s current overriding and super behavior and replace it
with BETA’s semantics. In short:
When calling a method on a class, prefer the method highest on
the class’s inheritance chain.
Inside the body of a method, a call to inner looks for a method
with the same name in the nearest subclass along the inheritance
chain between the class containing the inner and the class of
this. If there is no matching method, the inner call does nothing.
For example:

```python
class Doughnut {
 cook() {
 print "Fry until golden brown.";
 inner();
 print "Place in a nice box.";
 }
}
class BostonCream < Doughnut {
 cook() {
 print "Pipe full of custard and coat with chocolate.";
 }
}
BostonCream().cook();
```

This should print:

```log
Fry until golden brown.
Pipe full of custard and coat with chocolate.
Place in a nice box.
```

My Answer:
    - Interesting idea. In school, we all learn that a subclass inherits from a parent class. That parent class can be a subclass of another parent class and so on. Flipping this on its head is neat to think about. So I'd have an object of type B. B is a subclass class A. I'd call object.method(). The lookup would need to start at the top of the inheritance hierarchy. The way I'd implement this is have each class include a member pointing to the top of it's inheritance chain. So we'd look up method in object. We'd figure out method is not a field and search it's classes LUT. NOW, instead of actually immediately searching through B's method LUT, we'd check if B has a non-null "top of it's inheritance chain". Yes? Search there first. Look up 'method' in A's method LUT. If it's there, return that. If not, search the next highest class in the inheritance chain (in this case B).

    The other implementation piece would be implementing 'inner'. This means we'd need to add in a subclass field to LoxClass as well. Upon visiting 'inner', we'd likely need to use 'this' to access the current instance. Use the current instance to access the class the instance belongs to. Use the class to access the subclass. If a subclass exists, look up a method by the same name in that class. If not, look up return null (and visitInner does nothing). Something like that :) This means we'd also need to keep track of the current method being executed (since `inner` is used by itself without a method name unlike `super.method()`)

3) In the chapter where I introduced Lox, I challenged you to come up
with a couple of features you think the language is missing. Now that
you know how to build an interpreter, implement one of those features.

My Answer:
    - My plan was always to add arrays (or lists) so that's what I did!

    Parsing:
        - Added a rule for parsing lists:
        ```
            list -> "[" ( expression ("," expression )* )? "]"
        ``` 
        - NOTE: Ran into the comma rule ambiguity edge case again so only allow assignment+ expressions

        - Resolved each element of the list
        - Added `visitListExpr` to interpreter
            - Evaluated each Expr and added runtime Object to List<Object>
            - Created LoxList class that is essentially a wrapper for Java's ArrayList
            - Added Python-like list methods to LoxList
                - append
                - prepend
                - popFront
                - popBack
                - clear
                - isEmpty
                - size
        - TRICKY PART:
            - How do we call these list methods? LoxList isn't a LoxInstance...
            - I briefly considered doing this
            - I'd then need to statically add each of the above methods to a methods LUT in LoxList
                - sort of like the native functions are always added to Lox's globals env
            - This seemed kind of lame (and one of the reasons pple don't like OOP - should LoxList _really_ be inheriting from LoxInstance?)
            - I decided on a different approach
                - In `visitGetExpr`, I test if the object is a LoxList instance
                - If so, I use **Java's reflection feature**
                    - I call `getClass` on the LoxList instance
                        * This returns a `Class` object (containing LoxList metadata)
                    - Using the name of the method being called `getExpr.name.lexeme`, I call `getMethod` on the `Class` object
                        * This looks for a method by this name in the Class object
                    - Now, we have a reference to the LoxList method we're trying to invoke
                    - The last step is to return a LoxCallable that calls the method returned from `getMethod` upon being called (in visitCallExpr)
                        * This seemed weird to me at first b/c I didn't understand how the returned LoxCallable maintains a reference to the LoxList instance after leaving `visitGetExpr` (I was thinking the LoxList instance's lifetime lasted as long as the `visitGetExpr` call frame). It turns out, when using an anonymous Class instance (like LoxCallable), we have the ability to use Java's **variable capture** feature where the anonymous class object returned maintains a reference to the object being used (in this case, in LoxCallable's `call` method).
        - I also added support for indexing and slicing arrays in Lox
        - Also also added support for adding lists
            ```python
            [1] + [2]   // [1, 2]
            ```

8/10/2025:
    - Added support for index assignments for lists and maps
    - In Parser.assign(), added rule to detect if LHS is an IndexExpr
    - If so, return an IndexAssign expression

    - Changes:
        - Added `IndexAssign`, `Index[Pre|Post]fix` expressions in GenerateAst.java

        ```java
        // GenerateAst.java
        //...
        "IndexPrefix : Token operator, Expr object, Expr idxExpr",
        "IndexPostfix : Token operator, Expr object, Expr idxExpr",
        "IndexAssign : Token lbrack, Expr object, Expr idxExpr, Expr rhs",
        //...
        ```

        - Updated Parser to return `IndexAssign` in assign rule
            - Similar to Get -> Set trick, if LHS turns out to be Expr.Index, return an IndexAssign
        - Updated Parser to return `IndexPrefix` if object parsed in prefix rule turns out to be Expr.Index
        - Updated Parser to return `IndexPostfix if '++' or '--' are parsed in index rule
            - At first, I thought this code should be in postfix rule but that's hard to parse since Expr.Index will be returned from index rule before postfix() is called
            - Luckily, this doesn't break precedence since index is still higher precedence than prefix
        - Had to do a bunch of work in `visitBinaryExpr` to support binary operations using results of index operations
            - Before, was only supporting indexing for strings so I guess I get why this wasn't already there
        - `visitIndexAssign` determines the type of the object, evaluates the RHS and calls `set` for lists and `put` for maps
        - `visitIndexPrefix`does similar stuff at the start, then determines the type of the value being updated (value must be an int or a double). Based on this, it updates the value appropriately and updates the list / map as well.
        - `visitIndexPostfix` is the same except it returns the original value rather than the updated one