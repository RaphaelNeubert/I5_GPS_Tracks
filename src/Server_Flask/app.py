#to run server python app.py or -m flask run
import os 
from flask import Flask, flash, request, redirect, url_for
from werkzeug.utils import secure_filename
from flask import send_from_directory, send_file
from os.path import join, dirname, realpath

# folder for saving downloaded files
UPLOAD_FOLDER = join(dirname(realpath(__file__)), 'static\\upload\\')
# file extensions that are allowed to be uploaded
ALLOWED_EXTENSIONS = {'txt', 'pdf', 'png', 'jpg', 'jpeg', 'gif'}


app = Flask(__name__)

# upload folder 
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER


@app.route('/post',methods=["POST"])
def home_post():
    value=request.form['value']
    return (value)

#ipconfig  
 
@app.route('/upload', methods = ['POST'])  
def success():  
    if request.method == 'POST':  
        f = request.files['file']  
        f.save(f.filename)  
        #f.save(os.path.join(app.config['UPLOAD_FOLDER'],f.filename)) 
        return render_template(name = f.filename)  

@app.route('/download/<path:filename>', methods=['GET'])
def download(filename):
    return send_file(filename, as_attachment=True)
    #uploads = os.path.join(current_app.root_path, app.config['UPLOAD_FOLDER'])
    #return send_from_directory(directory=uploads, filename)
  
if __name__=="__main__":
    app.run(host='0.0.0.0')