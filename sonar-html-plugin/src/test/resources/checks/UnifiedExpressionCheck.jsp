<a foo="${foo.myMethod1()}" />
<a foo="${foo:myMethod1()}" />
<a foo="${foo:myMethod2()}" />

<a foo="${}" />

<a th:text="#{foo.bar(${foo.bar})}" </a>
