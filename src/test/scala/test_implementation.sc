import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.search.locators.Locators
import com.intellij.remoterobot.utils.Keyboard

import java.awt.event.KeyEvent
import java.io.{File, FileOutputStream, PrintWriter}
import java.lang.Thread.sleep
import javax.imageio.ImageIO
import scala.jdk.CollectionConverters.CollectionHasAsScala

// See also https://github.com/JetBrains/intellij-ui-test-robot/blob/master/README.md

val robot = new RemoteRobot("http://127.0.0.1:8082")
val keyboard = new Keyboard(robot)
def click(path: String) = {
  robot.find(classOf[ComponentFixture], Locators.byXpath(path)).click()
}
def click2(path: String) = {
  robot.find(classOf[ComponentFixture], Locators.byXpath(path)).doubleClick()
}
def clickr(path: String) = {
  robot.find(classOf[ComponentFixture], Locators.byXpath(path)).rightClick()
}
def multiline(input: String): Unit = {
  input.stripMargin.split("\n").map { line => () => keyboard.enterText(line) }.reduceLeft { (a, b) =>
    () => {
      a();
      keyboard.enter();
      b()
    }
  }()
}

def newJavaClass(name: String): Unit = {
  click("//div[@class='ProjectViewTree']")
  keyboard.key(KeyEvent.VK_RIGHT)
  keyboard.enterText("src")
  keyboard.key(KeyEvent.VK_CONTEXT_MENU)
  click("//div[contains(@text.key, 'group.NewGroup.text')]")
  sleep(100)
  click("//div[contains(@text.key, 'group.NewGroup.text')]//div[@text.key='action.NewClass.text']")
  keyboard.enterText(name)
  keyboard.enter()
}

def isDialogOpen = !robot.findAll(classOf[ComponentFixture], Locators.byXpath("//div[@class='EngravedLabel']")).isEmpty

def implementAndRunJavaClass(name: String, task: String) = {
  click("//div[@class='EditorComponentImpl']")
  keyboard.selectAll()
  keyboard.key(KeyEvent.VK_DELETE)
  multiline(
    """public class """ + name + """ {
      |  // """ + task)
  keyboard.key(KeyEvent.VK_CONTEXT_MENU)
  keyboard.key(KeyEvent.VK_A)
  keyboard.key(KeyEvent.VK_I)
  val image = robot.getScreenshot()
  keyboard.enter()
  while (!isDialogOpen) sleep(10)
  println("Dialog is open")
  while (isDialogOpen) sleep(100)
  println("Dialog is closed")
  keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_L)
  keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT, KeyEvent.VK_F10)
  image
}

val testOutputFile = new File("C:\\Users\\andre\\code\\aicoder\\intellij-aicoder\\build\\report.md")
val out = new PrintWriter(new FileOutputStream(testOutputFile))


def testTask(name: String, directive: String): Unit = {
  out.println("# " + name)
  out.println(
    s"""```
       |$directive
       |```""".stripMargin)
  newJavaClass(name)
  val menuImage = implementAndRunJavaClass(name, directive)
  ImageIO.write(menuImage, "png", new File(testOutputFile.getParentFile, name + "-menu.png"))
  sleep(30000)
  ImageIO.write(robot.getScreenshot(), "png", new File(testOutputFile.getParentFile, name + "-result.png"))
  out.println(
    s"""
       |![menu](${name}-menu.png)
       |![result](${name}-result.png)
       |```""".stripMargin)
  clickr("//div[@class='ContentTabLabel']")
  click("//div[contains(@text.key, 'action.CloseContent.text')]")
  click("//div[@class='InplaceButton']")
}


testTask("PrimeNumbers", "Print all prime numbers from 1 to 100")

println(robot.find(classOf[ComponentFixture], Locators.byXpath("//div[@class='EditorComponentImpl']")).getData.getAll.asScala.map(x=>x.getText).mkString(""))
println(robot.find(classOf[ComponentFixture], Locators.byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")).getData.getAll.asScala.map(x=>x.getText).mkString(""))


val editor = robot.find(classOf[ComponentFixture], Locators.byXpath("//div[@accessiblename.key='editor.for.file.accessible.name']"))
editor.callJs("java.util.Arrays.stream(robot.getClass().getMethods()).map((x)=>x.getName()).collect(java.util.stream.Collectors.joining(\"\\\n\"))")
java.util.Arrays.stream(robot.getClass().getMethods()).map(x=>x.getName()).collect(java.util.stream.Collectors.joining("\n"))


//robot.find(ComponentFixture::class.java, byXpath("//div[@accessiblename.key='editor.for.file.accessible.name']"))
robot.find(classOf[ComponentFixture], Locators.byXpath("//div[contains(@myvisibleactions, 'all')]//div[contains(@myaction.key, 'action.stop')]")).getData.


out.close()
