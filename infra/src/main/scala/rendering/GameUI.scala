package rendering

import actions.CardinalDirection
import model.{DndMapState, FightState}

import java.awt.{Color, Dimension, Font, Graphics2D, KeyEventDispatcher, RenderingHints}
import java.awt.event.KeyEvent
import scala.swing.*
import scala.swing.event.ButtonClicked

class GameUI extends MainFrame:
  title         = "E5 & Dragons"
  preferredSize = new Dimension(1050, 720)
  minimumSize   = new Dimension(950, 640)

  // ── Callbacks wired by Main ──────────────────────────────────────────────
  var onMove:  CardinalDirection => Unit = _ => ()
  var onFight: () => Unit                = () => ()

  // ── State ────────────────────────────────────────────────────────────────
  private var mapState: Option[DndMapState] = None

  // ── Map panel ────────────────────────────────────────────────────────────
  private val mapPanel: Panel = new Panel:
    preferredSize = new Dimension(680, 510)
    background    = new Color(12, 6, 2)

    override def paintComponent(g: Graphics2D): Unit =
      super.paintComponent(g)
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON)
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

      mapState.foreach: state =>
        val cols = state.width.max(1)
        val rows = state.height.max(1)
        val cW   = (size.width  - 4) / cols
        val cH   = (size.height - 4) / rows

        for y <- 0 until rows; x <- 0 until cols do
          val pos = (x, y)
          val cx  = 2 + x * cW
          val cy  = 2 + y * cH

          // Checkerboard floor
          g.setColor(if (x + y) % 2 == 0 then new Color(32, 18, 6) else new Color(24, 13, 4))
          g.fillRect(cx, cy, cW, cH)
          g.setColor(new Color(55, 35, 15))
          g.drawRect(cx, cy, cW, cH)

          // Entity
          val (bg, symbol, label): (Option[Color], String, String) =
            if state.playerPos == pos then
              (Some(new Color(20, 100, 210)), "\u265F", "YOU")
            else if state.villains.contains(pos) then
              (Some(new Color(175, 28, 28)), "\u2620", "ENE")
            else if state.npcPositions.contains(pos) then
              (Some(new Color(25, 145, 65)), "?", "NPC")
            else if state.goldPieces.contains(pos) then
              val amt = state.goldPieces(pos)
              (Some(new Color(175, 138, 8)), "\u2605", s"$amt")
            else
              (None, "", "")

          bg.foreach: c =>
            g.setColor(c)
            g.fillRoundRect(cx + 3, cy + 3, cW - 6, cH - 6, 14, 14)

            // Symbol (large)
            g.setColor(Color.WHITE)
            val symFont = new Font("SansSerif", Font.BOLD, (cW / 3).max(9).min(24))
            g.setFont(symFont)
            val sfm = g.getFontMetrics
            g.drawString(symbol,
              cx + (cW - sfm.stringWidth(symbol)) / 2,
              cy + cH / 2 + sfm.getAscent / 2 - 4)

            // Label (small, bottom)
            val lblFont = new Font("SansSerif", Font.PLAIN, (cW / 6).max(7).min(11))
            g.setFont(lblFont)
            val lfm = g.getFontMetrics
            g.drawString(label,
              cx + (cW - lfm.stringWidth(label)) / 2,
              cy + cH - 4)

          // Orientation arrow on player cell
          if state.playerPos == pos then
            val arrow = state.playerOrientation match
              case CardinalDirection.NORTH => "\u2191"
              case CardinalDirection.SOUTH => "\u2193"
              case CardinalDirection.EAST  => "\u2192"
              case CardinalDirection.WEST  => "\u2190"
            g.setColor(new Color(180, 220, 255))
            g.setFont(new Font("SansSerif", Font.PLAIN, (cW / 5).max(8)))
            g.drawString(arrow, cx + 4, cy + g.getFontMetrics.getAscent + 2)

  // ── Helpers for stat labels ──────────────────────────────────────────────
  private def mkLabel(txt: String, c: Color, bold: Boolean = false, fontSize: Int = 13) =
    new Label(txt):
      font       = new Font("SansSerif", if bold then Font.BOLD else Font.PLAIN, fontSize)
      foreground = c
      horizontalAlignment = Alignment.Left

  // ── Stats panel ──────────────────────────────────────────────────────────
  private val hpLabel   = mkLabel("HP: --",    new Color(230, 70, 70),  bold = true, fontSize =15)
  private val acLabel   = mkLabel("AC: --",    Color.LIGHT_GRAY)
  private val goldLabel = mkLabel("\u2605 Or: 0", new Color(210, 168, 20))
  private val posLabel  = mkLabel("(?,?)",     Color.GRAY, fontSize =11)

  private val statsPanel = new BoxPanel(Orientation.Vertical):
    border        = javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8)
    preferredSize = new Dimension(210, 510)
    val sep = new Separator()
    contents ++= Seq(
      mkLabel("\u2694 E5 & Dragons", new Color(220, 178, 50), bold = true, fontSize =17),
      new Label(" "),
      sep,
      new Label(" "),
      mkLabel("Personnage", Color.WHITE, bold = true),
      hpLabel, acLabel, goldLabel,
      new Label(" "),
      posLabel,
      new Label(" "),
      new Separator(),
      new Label(" "),
      mkLabel("Legende", Color.WHITE, bold = true),
      mkLabel("\u265F YOU  — vous",          new Color(80, 160, 255)),
      mkLabel("\u2620 ENE  — ennemi",        new Color(220, 80,  80)),
      mkLabel("?  NPC  — PNJ",              new Color(80, 200, 100)),
      mkLabel("\u2605 n     — or",            new Color(210, 168, 20)),
      new Label(" "),
      new Separator(),
      new Label(" "),
      mkLabel("Clavier", Color.WHITE, bold = true),
      mkLabel("\u2191\u2193\u2190\u2192  ou  WASD",  Color.GRAY, fontSize =11),
      mkLabel("F  = combattre",             Color.GRAY, fontSize =11)
    )

  // ── Log area ─────────────────────────────────────────────────────────────
  private val logArea = new TextArea(6, 55):
    editable  = false
    lineWrap  = true
    wordWrap  = true
    font      = new Font("Monospaced", Font.PLAIN, 12)
    background = new Color(8, 5, 2)
    foreground = new Color(155, 190, 110)

  private val logScroll = new ScrollPane(logArea):
    border = javax.swing.BorderFactory.createTitledBorder(
      javax.swing.BorderFactory.createLineBorder(new Color(55, 35, 15)),
      "Journal"
    )

  // ── Buttons ──────────────────────────────────────────────────────────────
  private def mkBtn(text: String, bg: Color) =
    new Button(text):
      focusable  = false
      font       = new Font("SansSerif", Font.BOLD, 13)
      background = bg
      foreground = Color.WHITE
      peer.setPreferredSize(new Dimension(95, 40))
      peer.setOpaque(true)

  private val btnN     = mkBtn("\u2191 Nord",   new Color(45, 30, 12))
  private val btnS     = mkBtn("\u2193 Sud",    new Color(45, 30, 12))
  private val btnE     = mkBtn("\u2192 Est",    new Color(45, 30, 12))
  private val btnW     = mkBtn("\u2190 Ouest",  new Color(45, 30, 12))
  private val btnFight = mkBtn("\u2694 Combat", new Color(120, 28, 28))

  // Cross layout: N on top, W-Fight-E in middle, S at bottom
  private val btnPanel = new BoxPanel(Orientation.Vertical):
    background = new Color(22, 13, 4)
    border     = javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4)
    contents ++= Seq(
      new FlowPanel(FlowPanel.Alignment.Center)(btnN):
        background = new Color(22, 13, 4),
      new FlowPanel(FlowPanel.Alignment.Center)(btnW, btnFight, btnE):
        background = new Color(22, 13, 4),
      new FlowPanel(FlowPanel.Alignment.Center)(btnS):
        background = new Color(22, 13, 4)
    )

  // ── Layout ───────────────────────────────────────────────────────────────
  private val topPanel = new BorderPanel:
    layout(mapPanel)   = BorderPanel.Position.Center
    layout(statsPanel) = BorderPanel.Position.East

  contents = new BorderPanel:
    background = new Color(18, 10, 3)
    layout(topPanel)  = BorderPanel.Position.Center
    layout(logScroll) = BorderPanel.Position.South
    layout(btnPanel)  = BorderPanel.Position.North

  // ── Button event wiring ──────────────────────────────────────────────────
  listenTo(btnN, btnS, btnE, btnW, btnFight)
  reactions += {
    case ButtonClicked(`btnN`)     => onMove(CardinalDirection.NORTH)
    case ButtonClicked(`btnS`)     => onMove(CardinalDirection.SOUTH)
    case ButtonClicked(`btnE`)     => onMove(CardinalDirection.EAST)
    case ButtonClicked(`btnW`)     => onMove(CardinalDirection.WEST)
    case ButtonClicked(`btnFight`) => onFight()
  }

  // ── Global keyboard support (arrows, WASD, F) ────────────────────────────
  // Uses KeyboardFocusManager so it works regardless of which component has focus.
  private val keyDispatcher: KeyEventDispatcher = (e: KeyEvent) =>
    if e.getID == KeyEvent.KEY_PRESSED then
      e.getKeyCode match
        case KeyEvent.VK_UP    | KeyEvent.VK_W => onMove(CardinalDirection.NORTH); true
        case KeyEvent.VK_DOWN  | KeyEvent.VK_S => onMove(CardinalDirection.SOUTH); true
        case KeyEvent.VK_RIGHT | KeyEvent.VK_D => onMove(CardinalDirection.EAST);  true
        case KeyEvent.VK_LEFT  | KeyEvent.VK_A => onMove(CardinalDirection.WEST);  true
        case KeyEvent.VK_F                     => onFight();                       true
        case _ => false
    else false

  java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager
    .addKeyEventDispatcher(keyDispatcher)

  // ── Public API ────────────────────────────────────────────────────────────
  def updateMap(state: DndMapState): Unit =
    mapState       = Some(state)
    hpLabel.text   = s"HP: ${state.player.hp}"
    acLabel.text   = s"AC: ${state.player.armorClass}"
    goldLabel.text = s"\u2605 Or: ${state.player.gold}"
    posLabel.text  = s"Pos: ${state.playerPos}  ${state.playerOrientation}"
    mapPanel.repaint()

  def updateFight(state: FightState): Unit =
    hpLabel.text = s"HP: ${state.playerHP.max(0)} / ${state.player.hp}"
    state.log.lastOption.foreach(appendLog)

  def appendLog(msg: String): Unit =
    logArea.append(s"> $msg\n")
    logArea.caret.position = logArea.text.length

  /** Blocking dialog shown after each combat — forces the player to read results. */
  def showCombatDialog(log: List[String], won: Boolean, playerHP: Int, villainHP: Int): Unit =
    val resultText = if won then "\u2694 Victoire !" else "\u2620 Defaite..."
    val area = new javax.swing.JTextArea(log.mkString("\n")):
      setEditable(false)
      setFont(new Font("Monospaced", Font.PLAIN, 12))
      setBackground(new Color(10, 6, 2))
      setForeground(new Color(155, 190, 110))
      setRows(14)
      setColumns(52)
    val scroll = new javax.swing.JScrollPane(area)
    val header = new javax.swing.JLabel(
      s"<html><b style='font-size:14px'>$resultText</b>" +
      s"<br/>Vos HP restants : <b>$playerHP</b>" +
      s"&nbsp;&nbsp;|&nbsp;&nbsp;HP Ennemi : <b>${villainHP.max(0)}</b><br/><br/></html>"
    )
    val panel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 6)):
      add(header, java.awt.BorderLayout.NORTH)
      add(scroll, java.awt.BorderLayout.CENTER)
    javax.swing.JOptionPane.showMessageDialog(
      peer, panel, "Résultat du combat", javax.swing.JOptionPane.PLAIN_MESSAGE
    )

  def askPlayAgain(): Boolean =
    val choice = javax.swing.JOptionPane.showConfirmDialog(
      peer,
      "\u2620 Vous êtes mort.\nVoulez-vous rejouer ?",
      "Game Over",
      javax.swing.JOptionPane.YES_NO_OPTION,
      javax.swing.JOptionPane.ERROR_MESSAGE
    )
    choice == javax.swing.JOptionPane.YES_OPTION

  def resetForNewGame(): Unit =
    logArea.text = ""
    hpLabel.text   = "HP: --"
    acLabel.text   = "AC: --"
    goldLabel.text = "\u2605 Or: 0"
    posLabel.text  = "(?,?)"
    mapState       = None
    mapPanel.repaint()

  override def closeOperation(): Unit =
    java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager
      .removeKeyEventDispatcher(keyDispatcher)
    sys.exit(0)
