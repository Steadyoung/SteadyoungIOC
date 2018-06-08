package Utils;

import View.FindViewByIdDialog;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction.Simple;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import entity.Element;
import org.apache.http.util.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class ViewInjectWriter extends Simple {

    /**
     * 初始化方法
     */
    private static final String VIEW_BIND = "ViewUtils.inject";

    private FindViewByIdDialog mDialog;
    private Editor mEditor;
    private PsiFile mFile;
    private Project mProject;
    private PsiClass mClass;
    private List<Element> mElements;
    private PsiElementFactory mFactory;

    public ViewInjectWriter(FindViewByIdDialog dialog, Editor editor, PsiFile psiFile, PsiClass psiClass, String command, List<Element> elements, String selectedText) {
        super(psiClass.getProject(), command);
        mDialog = dialog;
        mEditor = editor;
        mFile = psiFile;
        mProject = psiClass.getProject();
        mClass = psiClass;
        mElements = elements;
        // 获取Factory
        mFactory = JavaPsiFacade.getElementFactory(mProject);
    }

    @Override
    protected void run() throws Throwable {
        try {
            generateInjects();
            generateFields();
            generateOnClickMethod();
        } catch (Exception e) {
            // 异常打印
            mDialog.cancelDialog();
            Util.showPopupBalloon(mEditor, e.getMessage(), 10);
            return;
        }
        // 重写class
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mProject);
        styleManager.optimizeImports(mFile);
        styleManager.shortenClassReferences(mClass);
        new ReformatCodeProcessor(mProject, mClass.getContainingFile(), null, false).runWithoutProgress();
        Util.showPopupBalloon(mEditor, "生成成功", 5);
    }

    /**
     * 创建变量
     */
    private void generateFields() {
        for (Element element : mElements) {
            if (mClass.getText().contains("@ViewById(" + element.getFullID() + ")")) {
                // 不创建新的变量
                continue;
            }
            // 设置变量名，获取text里面的内容
            String text = element.getXml().getAttributeValue("android:text");
            if (TextUtils.isEmpty(text)) {
                // 如果是text为空，则获取hint里面的内容
                text = element.getXml().getAttributeValue("android:hint");
            }
            // 如果是@string/app_name类似
            if (!TextUtils.isEmpty(text) && text.contains("@string/")) {
                text = text.replace("@string/", "");
                // 获取strings.xml
                PsiFile[] psiFiles = FilenameIndex.getFilesByName(mProject, "strings.xml", GlobalSearchScope.allScope(mProject));
                if (psiFiles.length > 0) {
                    for (PsiFile psiFile : psiFiles) {
                        // 获取src\main\res\values下面的strings.xml文件
                        String dirName = psiFile.getParent().toString();
                        if (dirName.contains("src\\main\\res\\values")) {
                            text = Util.getTextFromStringsXml(psiFile, text);
                        }
                    }
                }
            }

            StringBuilder fromText = new StringBuilder();
            if (!TextUtils.isEmpty(text)) {
                fromText.append("/****" + text + "****/\n");
            }
            fromText.append("@ViewById(" + element.getFullID() + ")\n");
            fromText.append("private ");
            fromText.append(element.getName());
            fromText.append(" ");
            fromText.append(element.getFieldName());
            fromText.append(";");
            // 创建点击方法
            if (element.isCreateFiled()) {
                // 添加到class
                mClass.add(mFactory.createFieldFromText(fromText.toString(), mClass));
            }
        }
    }

    /**
     * 创建OnClick方法
     */
    private void generateOnClickMethod() {
        for (Element element : mElements) {
            // 可以使用并且可以点击
            if (element.isCreateClickMethod()) {
                // 需要创建OnClick方法
                String methodName = getClickMethodName(element) + "Click";
                PsiMethod[] onClickMethods = mClass.findMethodsByName(methodName, true);
                boolean clickMethodExist = onClickMethods.length > 0;
                if (!clickMethodExist) {
                    // 创建点击方法
                    createClickMethod(methodName, element);
                }
            }
        }
    }

    /**
     * 创建一个点击事件
     */
    private void createClickMethod(String methodName, Element element) {
        // 拼接方法的字符串
        StringBuilder methodBuilder = new StringBuilder();
        methodBuilder.append("@OnClick(" + element.getFullID() + ")\n");
        methodBuilder.append("private void " + methodName + "(" + element.getName() + " " + getClickMethodName(element) + "){");
        methodBuilder.append("\n}");
        // 创建OnClick方法
        mClass.add(mFactory.createMethodFromText(methodBuilder.toString(), mClass));
    }

    /**
     * 获取点击方法的名称
     */
    public String getClickMethodName(Element element) {
        String[] names = element.getId().split("_");
        // aaBbCc
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            if (i == 0) {
                sb.append(names[i]);
            } else {
                sb.append(Util.firstToUpperCase(names[i]));
            }
        }
        return sb.toString();
    }

    /**
     * 在加载布局后根据activity Fragement View 来初始化注解框架
     */
    private void generateInjects() {
        PsiClass activityClass = JavaPsiFacade.getInstance(mProject).findClass(
                "android.app.Activity", new EverythingGlobalScope(mProject));
        PsiClass fragmentClass = JavaPsiFacade.getInstance(mProject).findClass(
                "android.app.Fragment", new EverythingGlobalScope(mProject));
        PsiClass supportFragmentClass = JavaPsiFacade.getInstance(mProject).findClass(
                "android.support.v4.app.Fragment", new EverythingGlobalScope(mProject));

        // Check for Activity class
        if (activityClass != null && mClass.isInheritor(activityClass, true)) {
            generateActivityBind();
            // Check for Fragment class
        }
//        else if ((fragmentClass != null && mClass.isInheritor(fragmentClass, true)) || (supportFragmentClass != null && mClass.isInheritor(supportFragmentClass, true))) {
//            generateFragmentBindAndUnbind();
//        }
    }

    /**
     * activity在加载布局后生成ViewUtils.inject(this)代码
     */
    private void generateActivityBind() {
        PsiElementFactory mFactory = JavaPsiFacade.getElementFactory(mProject);
        if (mClass.findMethodsByName("onCreate", false).length == 0) {
            // Add an empty stub of onCreate()
            StringBuilder method = new StringBuilder();
            method.append("@Override protected void onCreate(android.os.Bundle savedInstanceState) {\n");
            method.append("super.onCreate(savedInstanceState);\n");
            method.append("\t// TODO: add setContentView(...) invocation\n");
            method.append(VIEW_BIND);
            method.append("(this);\n");
            method.append("}");

            mClass.add(mFactory.createMethodFromText(method.toString(), mClass));
        } else {
            PsiMethod onCreate = mClass.findMethodsByName("onCreate", false)[0];
            if (!containsViewInjectLine(onCreate, VIEW_BIND)) {
                for (PsiStatement statement : onCreate.getBody().getStatements()) {
                    // Search for setContentView()
                    if (statement.getFirstChild() instanceof PsiMethodCallExpression) {
                        PsiReferenceExpression methodExpression
                                = ((PsiMethodCallExpression) statement.getFirstChild())
                                .getMethodExpression();
                        // Insert ButterKnife.inject()/ButterKnife.bind() after setContentView()
                        if (methodExpression.getText().equals("setContentView")) {
                            onCreate.getBody().addAfter(mFactory.createStatementFromText(
                                    VIEW_BIND + "(this);", mClass), statement);
                            break;
                        }
                    }
                }
            }
        }
    }


    /**
     * 判断OnCreate中是否有初始化注解框架代码
     * @param method
     * @param line
     * @return
     */
    private boolean containsViewInjectLine(PsiMethod method, String line) {
        final PsiCodeBlock body = method.getBody();
        if (body == null) {
            return false;
        }
        PsiStatement[] statements = body.getStatements();
        for (PsiStatement psiStatement : statements) {
            String statementAsString = psiStatement.getText();
            if (psiStatement instanceof PsiExpressionStatement && (statementAsString.contains(line))) {
                return true;
            }
        }
        return false;
    }
}
