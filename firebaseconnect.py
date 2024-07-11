import firebase_admin
import base64
from firebase_admin import credentials, db
import time
import tkinter as tk
from PIL import Image, ImageTk
import PIL.Image
import threading

root = tk.Tk()



# Path to your service account key file
cred = credentials.Certificate('credentials.json')

# Initialize the app with the service account, specifying the project ID
firebase_admin.initialize_app(cred, {
    'projectId': 'agc-localhost-2c7f9',
    'databaseURL': 'http://127.0.0.1:9000/?ns=agc-localhost-2c7f9-default-rtdb',
})


ref = db.reference('command')
ref.set("off")

ref = db.reference('clicks/x')
ref.set("0")

ref = db.reference('clicks/y')
ref.set("0")


bytecode = ""
def on_screen():
    print("off")
    ref = db.reference('command')
    ref.set("off")



# Retrieve data from Realtime Database emulator
# Create a Canvas instead of a Label

canvas = tk.Canvas(root, width=450, height=750)
canvas.grid(row=0, column=0)

def startscreen():
    ref = db.reference('command')
    ref.set("startscreen")

    

button_start = tk.Button(root, text="start screen cast", command=startscreen)
button_start.grid(row=0, column=1, padx=10, pady=10)

button_on = tk.Button(root, text="on screen", command=on_screen)
button_on.grid(row=0, column=2)



def handle_click(event):
   
   # Retrieve the coordinates of the click event
    x = event.x
    y = event.y
    
   # Find the tags near the click position

    print(f"Clicked at ({x}, {y})")
    
    ref = db.reference('command')
    ref.set("click")

    string_x = str(x)
    string_y = str(y)


    ref = db.reference('clicks/x')
    ref.set(string_x)

    ref = db.reference('clicks/y')
    ref.set(string_y)
    


   # Perform actions with the nearby tags
   # ...

    

# Bind the mouse click event to the canvas widget


try:
    def update_image():
        global bytecode    
        ref = db.reference('images/key')

        users = ref.get()
        decodeit = open('hello_level.jpeg', 'wb') 
        decodeit.write(base64.b64decode((users))) 
        decodeit.close()

        if bytecode != users:
            img = Image.open('hello_level.jpeg')
            img = img.resize((450, 750), PIL.Image.Resampling.LANCZOS)

            tk_img = ImageTk.PhotoImage(img)
            # Delete previous image
            canvas.delete('all')
            # Add new image
            canvas.create_image(10, 10, anchor='nw', image=tk_img)
            # Keep a reference to the image to prevent it from being garbage collected
            canvas.image = tk_img
            bytecode = users

        # Schedule the next update
        root.after(100, update_image)
except:
    pass
canvas.bind("<Button-1>", handle_click)

# Start the updates
threading.Thread(target=update_image).start()
# Start the Tkinter event loop
root.mainloop()


