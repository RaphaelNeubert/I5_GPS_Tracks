#to run server python app.py or -m flask run
import os
from flask import Flask, flash, request, redirect, url_for
from werkzeug.utils import secure_filename
from flask import send_from_directory, send_file
from os.path import join, dirname, realpath

dirname = '/home/AleksandrPronin/mysite/files'
files = os.listdir(dirname)
temp = map(lambda name: os.path.join(dirname, name), files)
print(list(temp))

path ='/home/AleksandrPronin/mysite/files'
#we shall store all the file names in this list
filelist = []
for root, dirs, files in os.walk(path):
	for file in files:
        #append the file name to the list
		filelist.append(os.path.join(file))
		print (str(filelist))
#print all the file names
for name in filelist:
    print(name)


# folder for saving downloaded files
UPLOAD_FOLDER = '/home/AleksandrPronin/mysite/files'
# file extensions that are allowed to be uploaded
ALLOWED_EXTENSIONS = {'txt', 'pdf', 'png', 'jpg', 'jpeg', 'gif'}


app = Flask(__name__)

# upload folder
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

@app.route('/liste', methods=['GET'])
def liste_get():
    path ='/home/AleksandrPronin/mysite/files'
    #we shall store all the file names in this list
    filelist = []
    for root, dirs, files in os.walk(path):
    	for file in files:
            #append the file name to the list
    		filelist.append(os.path.join(file))
    return (str(filelist))

@app.route('/post',methods=["POsST"])
def home_post():
    value=request.form['value']
    return ("you")

#ipconfig

@app.route('/upload', methods = ['POST'])
def success():
    if request.method == 'POST':
        f = request.files['file']
        #f.save(f.filename)
        f.save(os.path.join(app.config['UPLOAD_FOLDER'],f.filename))
        return render_template(name = f.filename)

@app.route('/download/<path:filename>', methods=['GET'])
def download(filename):
    path = os.path.join(app.config['UPLOAD_FOLDER'],filename)
    return send_file(path, as_attachment=True)
    #uploads = os.path.join(current_app.root_path, app.config['UPLOAD_FOLDER'])
    #return send_from_directory(directory=uploads, filename)
  
if __name__=="__main__":
    app.run(host='0.0.0.0')