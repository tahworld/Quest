"""Quest Windows trial. Run with Python, or package on Windows with PyInstaller."""

import queue
import threading
import tkinter as tk
from tkinter import messagebox, ttk

from ai import generate
from storage import Store


BG = "#f5f9f6"
INK = "#20322d"
MINT = "#d2ecdf"
GREEN = "#397866"
MUTED = "#5b6f66"


class QuestApp:
    def __init__(self, root, store=None):
        self.root = root
        self.store = store or Store()
        self.root.title("Quest · 今天的一步")
        self.root.geometry("860x760")
        self.root.minsize(690, 650)
        self.root.configure(bg=BG)
        self.direction = self.store.direction()
        self.intention = ""
        self.minutes = tk.IntVar(value=15)
        self.energy = tk.IntVar(value=3)
        self.resources = {name: tk.BooleanVar(value=name == "手机") for name in
                          ("手机", "电脑", "安静思考", "可以活动")}
        self.api_key = ""  # Session only: never write a secret to SQLite or the artifact.
        self.model = "deepseek-flash"
        self.base_url = "https://api.deepseek.com"
        self.busy = False
        self.events = queue.Queue()
        self.panel = tk.Frame(root, bg=BG)
        self.panel.pack(fill="both", expand=True, padx=42, pady=27)
        self.root.after(100, self.poll)
        self.show_home()

    def clear(self):
        for widget in self.panel.winfo_children():
            widget.destroy()

    def label(self, text, size=12, color=INK, bold=False, pady=(0, 8)):
        widget = tk.Label(self.panel, text=text, bg=BG, fg=color, anchor="w", justify="left",
                          font=("Microsoft YaHei UI", size, "bold" if bold else "normal"),
                          wraplength=760)
        widget.pack(fill="x", pady=pady)
        return widget

    def button(self, parent, title, action, primary=True):
        widget = tk.Button(parent, text=title, command=action, relief="flat", cursor="hand2",
                           bg=GREEN if primary else MINT, fg="white" if primary else INK,
                           activebackground="#275a4d", activeforeground="white",
                           font=("Microsoft YaHei UI", 11, "bold"), padx=20, pady=11)
        widget.pack(side="left", padx=(0, 12), pady=4)
        return widget

    def current_conditions(self):
        return {"minutes": self.minutes.get(), "energy": self.energy.get(),
                "resources": [name for name, value in self.resources.items() if value.get()]}

    def remember_form(self):
        if hasattr(self, "direction_entry") and self.direction_entry.winfo_exists():
            self.direction = self.direction_entry.get().strip()
            self.intention = self.intention_text.get("1.0", "end").strip()
            self.api_key = self.key_entry.get().strip()
            self.model = self.model_entry.get().strip()
            self.base_url = self.url_entry.get().strip()

    def show_home(self):
        self.clear()
        self.label("Quest", 17, GREEN, True, (0, 22))
        self.label("今天最容易开始的一步是什么？", 22, INK, True, (0, 20))
        self.label("你想坚持的方向", 11, MUTED)
        self.direction_entry = ttk.Entry(self.panel, font=("Microsoft YaHei UI", 12))
        self.direction_entry.insert(0, self.direction)
        self.direction_entry.pack(fill="x", ipady=7, pady=(0, 17))
        self.label("此刻的想法（可以留空）", 11, MUTED)
        self.intention_text = tk.Text(self.panel, height=4, wrap="word", relief="solid", borderwidth=1,
                                      font=("Microsoft YaHei UI", 11), bg="white", fg=INK)
        self.intention_text.insert("1.0", self.intention)
        self.intention_text.pack(fill="x", pady=(0, 16))

        row = tk.Frame(self.panel, bg=BG)
        row.pack(fill="x", pady=(0, 14))
        tk.Label(row, text="可用时间", bg=BG, fg=INK, font=("Microsoft YaHei UI", 11)).pack(side="left")
        for minute in (5, 15, 30, 60):
            tk.Radiobutton(row, text=f"{minute} 分钟", value=minute, variable=self.minutes,
                           bg=BG, fg=INK, selectcolor=MINT, font=("Microsoft YaHei UI", 10)).pack(side="left", padx=7)
        row = tk.Frame(self.panel, bg=BG)
        row.pack(fill="x", pady=(0, 14))
        tk.Label(row, text="精力", bg=BG, fg=INK, font=("Microsoft YaHei UI", 11)).pack(side="left")
        for level in range(1, 6):
            tk.Radiobutton(row, text=str(level), value=level, variable=self.energy,
                           bg=BG, fg=INK, selectcolor=MINT, font=("Microsoft YaHei UI", 10)).pack(side="left", padx=12)
        row = tk.Frame(self.panel, bg=BG)
        row.pack(fill="x", pady=(0, 22))
        tk.Label(row, text="当前条件", bg=BG, fg=INK, font=("Microsoft YaHei UI", 11)).pack(side="left")
        for name, value in self.resources.items():
            tk.Checkbutton(row, text=name, variable=value, bg=BG, fg=INK,
                           selectcolor=MINT, font=("Microsoft YaHei UI", 10)).pack(side="left", padx=9)

        actions = tk.Frame(self.panel, bg=BG)
        actions.pack(fill="x")
        self.generate_button = self.button(actions, "给我今天的一步", self.request_quest)
        active = self.store.latest_active()
        if active:
            self.button(actions, "继续进行中的行动", lambda: self.show_quest(active["id"]), False)
        self.status = self.label("", 10, MUTED, pady=(6, 18))

        settings = ttk.LabelFrame(self.panel, text="AI 设置 · 测试时由使用者填写自己的 Key")
        settings.pack(fill="x", pady=(0, 10))
        settings.columnconfigure(1, weight=1)
        for index, (caption, saved, secret) in enumerate((
            ("DeepSeek API Key", self.api_key, True),
            ("模型", self.model, False),
            ("Base URL", self.base_url, False),
        )):
            ttk.Label(settings, text=caption).grid(row=index, column=0, padx=12, pady=7, sticky="w")
            entry = ttk.Entry(settings, show="•" if secret else "")
            entry.insert(0, saved)
            entry.grid(row=index, column=1, padx=12, pady=7, sticky="ew")
            if index == 0:
                self.key_entry = entry
            elif index == 1:
                self.model_entry = entry
            else:
                self.url_entry = entry
        self.label("Key 仅保留在本次运行的内存中。留空时使用离线示例，无法检验真实模型质量。", 9, MUTED)

    def request_quest(self):
        if self.busy:
            return
        self.remember_form()
        if not self.direction:
            messagebox.showinfo("先选方向", "先写下你想坚持的方向，例如：每天读书或规律运动。")
            return
        if not self.current_conditions()["resources"]:
            messagebox.showinfo("当前条件", "至少选择一项当前可用的条件。")
            return
        self.store.save_direction(self.direction)
        direction, intention, conditions = self.direction, self.intention, self.current_conditions()
        recent, rejections = self.store.recent(), self.store.recent_rejections()
        config = (self.api_key, self.model, self.base_url)
        self.busy = True
        self.generate_button.config(state="disabled")
        self.status.config(text="正在生成一条可执行的行动……", fg=GREEN)

        def work():
            try:
                draft = generate(direction, intention, conditions, recent, rejections,
                                 api_key=config[0], model=config[1], base_url=config[2])
                quest_id = self.store.create_quest(direction, intention, conditions, draft)
                self.events.put(("quest", quest_id))
            except Exception as error:
                self.events.put(("error", str(error)))
        threading.Thread(target=work, daemon=True).start()

    def poll(self):
        try:
            while True:
                kind, payload = self.events.get_nowait()
                self.busy = False
                if kind == "quest":
                    self.show_quest(payload)
                else:
                    if hasattr(self, "generate_button") and self.generate_button.winfo_exists():
                        self.generate_button.config(state="normal")
                        self.status.config(text=payload, fg="#af4141")
                    else:
                        messagebox.showerror("生成失败", payload)
        except queue.Empty:
            pass
        self.root.after(100, self.poll)

    def show_quest(self, quest_id):
        quest = self.store.quest(quest_id)
        if not quest:
            self.show_home()
            return
        self.clear()
        draft = quest["draft"]
        self.label("QUEST · " + quest["direction"], 11, GREEN, True, (0, 24))
        self.label(draft["title"], 22, INK, True, (0, 12))
        self.label(f"约 {draft['estimatedMinutes']} 分钟   ·   {quest['status']}", 10, MUTED, pady=(0, 18))
        self.label(draft["reason"], 11, MUTED, pady=(0, 20))
        self.label("怎么做", 13, INK, True)
        for index, step in enumerate(draft["steps"], 1):
            self.label(f"{index}.  {step}", 11, pady=(0, 9))
        self.label("做到什么算完成", 13, INK, True, (15, 8))
        for criterion in draft["completionCriteria"]:
            self.label("•  " + criterion, 11, pady=(0, 8))
        self.label("留下什么", 13, INK, True, (15, 8))
        self.label(draft["expectedOutput"], 11, pady=(0, 26))
        row = tk.Frame(self.panel, bg=BG)
        row.pack(fill="x")
        if quest["status"] == "PENDING":
            self.button(row, "开始行动", lambda: self.start_quest(quest_id))
            self.button(row, "换一个", lambda: self.reroll(quest_id), False)
            self.button(row, "调整条件", self.show_home, False)
        elif quest["status"] == "ACTIVE":
            self.button(row, "完成并记录", lambda: self.complete(quest_id))
            self.button(row, "放弃这次", lambda: self.abandon(quest_id), False)
            self.button(row, "返回首页", self.show_home, False)
        else:
            self.label("这条行动已结束。", 11, MUTED)
            self.button(row, "返回首页", self.show_home, False)

    def start_quest(self, quest_id):
        if self.store.transition(quest_id, "PENDING", "ACTIVE"):
            self.show_quest(quest_id)

    def reroll(self, quest_id):
        reasons = ("现在做不了", "不感兴趣", "太简单", "太困难", "价值低", "已经做过", "只想换一个", "其他")
        dialog = tk.Toplevel(self.root)
        dialog.title("为什么换一个？")
        dialog.transient(self.root)
        dialog.grab_set()
        choice = tk.StringVar(value=reasons[0])
        for reason in reasons:
            tk.Radiobutton(dialog, text=reason, value=reason, variable=choice,
                           font=("Microsoft YaHei UI", 10)).pack(anchor="w", padx=22, pady=3)

        def confirm():
            if self.busy:
                return
            if self.store.reject(quest_id, choice.get()):
                dialog.destroy()
                self.show_home()
                self.request_quest()
        ttk.Button(dialog, text="确认并重新生成", command=confirm).pack(padx=22, pady=15)

    def complete(self, quest_id):
        dialog = tk.Toplevel(self.root)
        dialog.title("完成行动")
        dialog.geometry("520x300")
        dialog.transient(self.root)
        dialog.grab_set()
        ttk.Label(dialog, text="你做出了什么，或发现了什么？").pack(anchor="w", padx=22, pady=(20, 9))
        result = tk.Text(dialog, height=8, wrap="word", font=("Microsoft YaHei UI", 11))
        result.pack(fill="both", expand=True, padx=22)

        def save():
            content = result.get("1.0", "end").strip()
            if not content:
                messagebox.showinfo("需要结果", "写下一条实际结果，方便下一次行动接续。", parent=dialog)
                return
            if self.store.transition(quest_id, "ACTIVE", "COMPLETED", content):
                dialog.destroy()
                self.show_quest(quest_id)
        ttk.Button(dialog, text="保存结果", command=save).pack(pady=16)

    def abandon(self, quest_id):
        if messagebox.askyesno("放弃这次", "这次行动先结束？", parent=self.root):
            if self.store.transition(quest_id, "ACTIVE", "ABANDONED"):
                self.show_home()


if __name__ == "__main__":
    window = tk.Tk()
    QuestApp(window)
    window.mainloop()
