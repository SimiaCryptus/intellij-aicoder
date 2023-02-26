import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText
import com.intellij.remoterobot.search.locators.Locators
import com.intellij.remoterobot.utils.Keyboard

import java.awt.Point
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
    """public class """ + name +
      """ {
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

println(robot.find(classOf[ComponentFixture], Locators.byXpath("//div[@class='EditorComponentImpl']")).getData.getAll.asScala.map(x => x.getText).mkString(""))
println(robot.find(classOf[ComponentFixture], Locators.byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")).getData.getAll.asScala.map(x => x.getText).mkString(""))


val editor = robot.find(classOf[ComponentFixture], Locators.byXpath("//div[@class='EditorComponentImpl']"))

sleep(5000)
editor.runJs("robot.moveMouse(component)")

editor.callJs("java.util.Arrays.stream(robot.getClass().getMethods()).map((x)=>x.getName()).collect(java.util.stream.Collectors.joining(\"\\\n\"))")
java.util.Arrays.stream(robot.getClass().getMethods()).map(x => x.getName()).collect(java.util.stream.Collectors.joining("\n"))

val data = editor.getData
println(data.getAll.asScala.groupBy(_.getPoint.y).toList.sortBy(_._1).map(_._2).map(_.toList.sortBy(_.getPoint.x).map(_.getText).mkString("")).mkString("\n"))


//robot.find(ComponentFixture::class.java, byXpath("//div[@accessiblename.key='editor.for.file.accessible.name']"))
robot.find(classOf[ComponentFixture], Locators.byXpath("//div[contains(@myvisibleactions, 'all')]//div[contains(@myaction.key, 'action.stop')]")).getData;

sleep(5000)
editor.click()
keyboard.key(KeyEvent.VK_CONTEXT_MENU)
keyboard.key(KeyEvent.VK_A)
robot.find(classOf[ComponentFixture], Locators.byXpath("//div[@class='HeavyWeightWindow'][.//div[@class='MyMenu']]//div[@class='HeavyWeightWindow']//div[contains(@text, 'Describe Code')]")).runJs("robot.moveMouse(component)")


def findText(element: ComponentFixture, text: String) = {
  val lines: Map[Int, Iterable[RemoteText]] = element.getData.getAll().asScala.groupBy(_.getPoint.y)
  val line = lines.toList.filter(_._2.map(_.getText).reduce(_ + _).contains(text)).head._2
  val index = line.map(_.getText).reduce(_ + _).indexOf(text)
  val lineBuffer = line.toBuffer
  var left = index
  while (left > 0) {
    val first = lineBuffer.head
    if (first.getText.length <= left) {
      left -= first.getText.length
      lineBuffer.remove(0)
    }
  }
  val leftPoint = lineBuffer.head.getPoint
  left += text.length
  while (left > 0) {
    val first = lineBuffer.head
    if (first.getText.length <= left) {
      left -= first.getText.length
      lineBuffer.remove(0)
    }
  }
  val rightPoint = lineBuffer.head.getPoint
  (leftPoint, rightPoint)
}
def selectText(text:String): Unit = {
  val (leftPoint, rightPoint) = findText(editor, text)
  editor.runJs(s"robot.pressMouse(component, new Point(${leftPoint.x}, ${leftPoint.y}))")
  editor.runJs(s"robot.moveMouse(component, new Point(${rightPoint.x}, ${rightPoint.y}))")
  editor.runJs(s"robot.releaseMouseButtons()")
}

sleep(2000)
selectText("public static")
